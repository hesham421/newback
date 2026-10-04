"""Minimal SMTP capture sink (stdlib only) for the P-MAIL profile of core_api_verify.py.

Python 3.12+ has no `smtpd`, so this is a small socket-based receiver. It speaks just enough
ESMTP for JavaMail (EHLO/HELO, MAIL, RCPT, DATA, RSET, NOOP, QUIT), never offers AUTH or
STARTTLS, and stores every received message as `<n>.eml` in a folder. Intended for local
Dev/Test use only.

    python docs/test-api/smtp_sink.py --port 1025 --dir <folder>        # standalone
    from smtp_sink import SmtpSink; sink = SmtpSink(1025, folder).start()  # in-process
"""
from __future__ import annotations

import argparse
import email
import email.policy
import itertools
import os
import socket
import socketserver
import threading
import time

_counter = itertools.count(1)
_lock = threading.Lock()


class _Handler(socketserver.StreamRequestHandler):
    def _send(self, line: str) -> None:
        self.wfile.write((line + "\r\n").encode("ascii"))
        self.wfile.flush()

    def handle(self) -> None:  # noqa: C901 - a tiny state machine
        self._send("220 erp-verify-sink ESMTP")
        mail_from, rcpts = None, []
        while True:
            raw = self.rfile.readline()
            if not raw:
                return
            line = raw.decode("utf-8", "replace").rstrip("\r\n")
            cmd = line[:4].upper()
            if cmd == "EHLO":
                self._send("250-erp-verify-sink")
                self._send("250-8BITMIME")
                self._send("250 SMTPUTF8")
            elif cmd == "HELO":
                self._send("250 erp-verify-sink")
            elif cmd == "MAIL":
                mail_from, rcpts = line[10:].strip(), []
                self._send("250 OK")
            elif cmd == "RCPT":
                rcpts.append(line[8:].strip().strip("<>"))
                self._send("250 OK")
            elif cmd == "DATA":
                self._send("354 End data with <CR><LF>.<CR><LF>")
                chunks = []
                while True:
                    d = self.rfile.readline()
                    if not d or d in (b".\r\n", b".\n"):
                        break
                    if d.startswith(b".."):
                        d = d[1:]
                    chunks.append(d)
                self.server.store(mail_from, rcpts, b"".join(chunks))  # type: ignore[attr-defined]
                self._send("250 OK queued")
            elif cmd == "RSET":
                mail_from, rcpts = None, []
                self._send("250 OK")
            elif cmd == "NOOP":
                self._send("250 OK")
            elif cmd == "QUIT":
                self._send("221 Bye")
                return
            else:
                self._send("502 Command not implemented")


class _Server(socketserver.ThreadingTCPServer):
    allow_reuse_address = True
    daemon_threads = True

    def __init__(self, addr, folder):
        super().__init__(addr, _Handler)
        self.folder = folder

    def store(self, mail_from, rcpts, data: bytes) -> None:
        with _lock:
            n = next(_counter)
            while os.path.exists(os.path.join(self.folder, f"{n:05d}.eml")):
                n = next(_counter)
            path = os.path.join(self.folder, f"{n:05d}.eml")
            header = f"X-Sink-From: {mail_from}\r\nX-Sink-Rcpt: {','.join(rcpts)}\r\n".encode()
            with open(path, "wb") as fh:
                fh.write(header + data)


class SmtpSink:
    def __init__(self, port: int, folder: str, host: str = "127.0.0.1"):
        os.makedirs(folder, exist_ok=True)
        self.folder = folder
        self.server = _Server((host, port), folder)
        self.thread = threading.Thread(target=self.server.serve_forever, daemon=True)

    def start(self) -> "SmtpSink":
        self.thread.start()
        return self

    def stop(self) -> None:
        self.server.shutdown()
        self.server.server_close()

    # ---- reading captured mail -------------------------------------------------------------
    def messages(self):
        out = []
        for name in sorted(os.listdir(self.folder)):
            if not name.endswith(".eml"):
                continue
            with open(os.path.join(self.folder, name), "rb") as fh:
                msg = email.message_from_binary_file(fh, policy=email.policy.default)
            out.append(msg)
        return out

    @staticmethod
    def text_of(msg) -> str:
        parts = []
        for part in msg.walk():
            if part.get_content_maintype() == "text":
                try:
                    parts.append(part.get_content())
                except Exception:  # pragma: no cover - defensive
                    parts.append(part.get_payload(decode=True).decode("utf-8", "replace"))
        return "\n".join(parts)

    def wait_for(self, predicate, timeout: float = 20.0):
        deadline = time.time() + timeout
        while time.time() < deadline:
            for m in self.messages():
                if predicate(m):
                    return m
            time.sleep(0.3)
        return None


def port_is_free(port: int, host: str = "127.0.0.1") -> bool:
    with socket.socket() as s:
        return s.connect_ex((host, port)) != 0


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--port", type=int, default=1025)
    ap.add_argument("--dir", required=True)
    a = ap.parse_args()
    SmtpSink(a.port, a.dir).start()
    print(f"SMTP sink on 127.0.0.1:{a.port}, storing into {a.dir}")
    try:
        while True:
            time.sleep(3600)
    except KeyboardInterrupt:
        pass
