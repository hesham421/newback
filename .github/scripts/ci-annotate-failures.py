#!/usr/bin/env python3
"""Turns a failed `mvn verify` into GitHub annotations (erp-core step 12, post-merge CI fix).

Job logs of this public repository can only be downloaded with a token, but check-run
annotations are public (GET /repos/{owner}/{repo}/check-runs/{job_id}/annotations). This script
therefore puts the root cause of a red build into annotations:

* one summary annotation: totals over every surefire/failsafe report, the Maven "Failed to execute
  goal" line, and the id of every failed/errored test;
* one annotation per failed/errored test (class.method, file + line when the test source is found,
  the message and the first lines of the stack trace);
* when no report holds a failed test (enforcer, compilation, JaCoCo gate, a crashed fork, a
  container that never started), the first relevant `[ERROR]` lines of the Maven log.

GitHub shows at most 10 annotations per level and step (and 50 per job), so per-test annotations
are spread over error, warning and notice; the summary still names every failed test.

Usage: ci-annotate-failures.py <maven log file>   (run from the repository root)
"""
import glob
import os
import re
import sys
import xml.etree.ElementTree as ET

STACK_LINES = 15
MAX_BODY_CHARS = 3500
MAX_MAVEN_ERROR_LINES = 30
PER_LEVEL = 10
LEVELS = ("error", "warning", "notice")
MAX_PER_TEST = PER_LEVEL * len(LEVELS) - 1  # one error slot is taken by the summary

MAVEN_NOISE = re.compile(
    r"^\[ERROR\]\s*$|-> \[Help 1\]|Re-run Maven|To see the full stack trace|For more information about"
    r"|\[Help 1\] http|After correcting the problems|mvn <args> -rf|See dump files"
    r"|for the individual test results")


def esc_data(value):
    return value.replace("%", "%25").replace("\r", "%0D").replace("\n", "%0A")


def esc_prop(value):
    return esc_data(value).replace(":", "%3A").replace(",", "%2C")


def emit(level, message, title=None, file=None, line=None):
    props = []
    if file:
        props.append("file=" + esc_prop(file))
        if line:
            props.append("line=" + str(line))
    if title:
        props.append("title=" + esc_prop(title))
    head = "::" + level + (" " + ",".join(props) if props else "") + "::"
    print(head + esc_data(message), flush=True)


def source_file(classname):
    outer = classname.split("$", 1)[0]
    rel = outer.replace(".", "/") + ".java"
    for candidate in glob.glob("**/src/test/java/" + rel, recursive=True):
        return candidate.replace(os.sep, "/")
    return None


def line_in(stack, classname):
    outer = classname.split("$", 1)[0]
    simple = outer.rsplit(".", 1)[-1]
    match = re.search(r"at " + re.escape(outer) + r"[.$][^(]*\(" + re.escape(simple) + r"\.java:(\d+)\)", stack)
    return int(match.group(1)) if match else None


def collect():
    totals = {"tests": 0, "failures": 0, "errors": 0, "skipped": 0}
    failed = []
    reports = sorted(glob.glob("**/target/surefire-reports/TEST-*.xml", recursive=True)
                     + glob.glob("**/target/failsafe-reports/TEST-*.xml", recursive=True))
    for path in reports:
        try:
            root = ET.parse(path).getroot()
        except ET.ParseError as e:
            failed.append(("report", os.path.basename(path), "unparseable report: " + str(e), ""))
            continue
        suites = [root] if root.tag == "testsuite" else root.findall("testsuite")
        for suite in suites:
            for key in totals:
                try:
                    totals[key] += int(suite.get(key, "0"))
                except ValueError:
                    pass
            for case in suite.iter("testcase"):
                for kind in ("failure", "error"):
                    node = case.find(kind)
                    if node is None:
                        continue
                    classname = case.get("classname") or suite.get("name") or "?"
                    name = case.get("name") or "?"
                    message = (node.get("message") or "").strip()
                    stack = (node.text or "").strip()
                    failed.append((classname, name, message, stack, kind))
    return reports, totals, failed


def maven_error_lines(log_path):
    if not log_path or not os.path.exists(log_path):
        return []
    lines = []
    with open(log_path, encoding="utf-8", errors="replace") as handle:
        for raw in handle:
            line = re.sub(r"\x1b\[[0-9;]*m", "", raw.rstrip("\n"))
            if line.startswith("[ERROR]") and not MAVEN_NOISE.search(line):
                lines.append(line)
    return lines


def main():
    log_path = sys.argv[1] if len(sys.argv) > 1 else None
    reports, totals, failed = collect()
    maven_errors = maven_error_lines(log_path)
    goal_line = next((l for l in maven_errors if "Failed to execute goal" in l), None)

    real = [f for f in failed if f[0] != "report"]
    summary = ("{} report file(s): {tests} tests, {failures} failures, {errors} errors, {skipped} skipped"
               .format(len(reports), **totals))
    lines = [summary]
    if goal_line:
        lines.append(goal_line)
    if failed:
        lines.append("Failed / errored tests:")
        lines.extend("  {}.{}".format(f[0], f[1]) for f in failed)
    emit("error", "\n".join(lines)[:MAX_BODY_CHARS * 4], title="mvn verify failed - summary")

    step_summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if step_summary:
        with open(step_summary, "a", encoding="utf-8") as out:
            out.write("### mvn verify failed\n\n" + "\n".join("    " + l for l in lines) + "\n")

    for index, entry in enumerate(real[:MAX_PER_TEST]):
        classname, name, message, stack, kind = entry
        level = LEVELS[min((index + 1) // PER_LEVEL, len(LEVELS) - 1)]
        stack_head = "\n".join(stack.splitlines()[:STACK_LINES])
        body = (message + "\n" + stack_head if message and message not in stack_head else stack_head or message)
        emit(level, "[" + kind + "] " + body[:MAX_BODY_CHARS],
             title="{}.{}".format(classname.rsplit(".", 1)[-1], name),
             file=source_file(classname), line=line_in(stack, classname))
    if len(real) > MAX_PER_TEST:
        print("{} more failed test(s) not annotated (GitHub's per-step annotation limit); see the summary."
              .format(len(real) - MAX_PER_TEST))

    if not real:
        # Nothing failed inside a test: the root cause is in the Maven output itself.
        relevant = maven_errors[:MAX_MAVEN_ERROR_LINES]
        if not relevant:
            emit("warning", "no failed test in the reports and no [ERROR] line in " + str(log_path))
        for index, line in enumerate(relevant):
            emit(LEVELS[min((index + 1) // PER_LEVEL, len(LEVELS) - 1)], line, title="maven [ERROR]")
    return 0


if __name__ == "__main__":
    sys.exit(main())
