-- ============================================================================
-- oracle-aq-setup.sql — accounting event queue (Oracle, schema LOAN_SYS)         [file 1 of 3]
-- ============================================================================
-- Prepared 2026-09-23 against LOAN_SYS@FREEPDB1 (Oracle AI Database 26ai Free 23.26.2.0.0,
-- compatible = 23.6.0, DBMS_AQADM.CREATE_TRANSACTIONAL_EVENT_QUEUE present — verified read-only).
-- Design: governance/project-artifacts/oracle-aq-fin-integration-plan.md §2.1, §2.3, §3.2, §3.4, §3.5.
-- Order on production: 1 this file → 2 oracle-event-emit-package.sql → 3 oracle-event-emit-triggers.sql
--                      → (consumer running, shadow verified) → legacy-accounting-stop.sql.
--
-- WHAT THIS CREATES (all additive, all reversible — see ROLLBACK at the end)
--   ACCOUNTING_EVENT_SEQ   sequence — the per-emission suffix of eventReference (uniqueness without
--                          adding a revision column to any legacy table)
--   ACCOUNTING_EVENT_Q     Transactional Event Queue, JSON payload, single consumer, single shard
--                          (= global FIFO), max_retries 5.
--   ACCOUNTING_EVENT_Q_EXCPT its exception queue (section 3b) — NOT automatic on a TxEventQ;
--                          without it a message past max_retries is purged.
--                          retry_delay is not a TxEventQ setting — the consumer owns back-off.
--   ACCOUNTING_EVENT_LOG   a plain table: every emission's reference, type, payload and outcome.
--                          The queue is the transport; this is the audit and the replay source.
--                          (governance plan §3.3 wants a consumer-side inbox too — that lives in
--                          PostgreSQL, not here.)
--
-- WHY TxEventQ AND NOT CLASSIC AQ
--   Oracle's current guidance for new applications on 21c+/23ai is TxEventQ (plan §2.1). The
--   enqueue call (DBMS_AQ.ENQUEUE) and the JMS consumer are identical for both, so the only
--   thing that would change on an edition without TxEventQ is this file — see the FALLBACK block.
--
-- WHY ONE CONSUMER, FIFO
--   Per-document ordering is a correctness property (an update = reversal + new entry — plan §3.5).
--   A single dequeuing thread on a FIFO queue guarantees it without message groups. 2026 volume
--   (≈ 4,800 journal-producing rows in 9 months) is far below what one thread handles.
--
-- HOW TO RUN     sqlplus LOAN_SYS/...@//host:1521/FREEPDB1 @oracle-aq-setup.sql   (as LOAN_SYS; it holds DBA)
-- IDEMPOTENT     every step checks for the object first and skips it if present.
-- ============================================================================

SET ECHO ON
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT FAILURE

-- ----------------------------------------------------------------------------
-- 0. Direct EXECUTE on DBMS_AQ — a PREREQUISITE, not a nicety
-- ----------------------------------------------------------------------------
-- LOAN_SYS holds DBA, but only as a ROLE, and it has no direct object grants at all
-- (user_tab_privs on DBMS_AQ/DBMS_AQADM: no rows). Roles are active in an anonymous block and
-- disabled inside a definer-rights stored unit, so DBMS_AQADM calls in this file succeed while
-- PKG_ACCOUNTING_EVENT's body fails to compile with
--     PLS-00201: identifier 'DBMS_AQ' must be declared
-- Discovered on the first real run, 2026-09-23. A direct grant is the documented AQ requirement.
-- It CANNOT be issued by LOAN_SYS itself — ORA-01749 "Cannot GRANT or REVOKE privileges to or from
-- yourself" (hit 2026-09-24), DBA role or not. A DBA runs it as SYS, in the PDB, BEFORE this file:
--     -- on the test copy (container, OS authentication):
--     docker exec -i erp-oracle bash -lc 'sqlplus -S / as sysdba <<EOF
--     ALTER SESSION SET CONTAINER = FREEPDB1;
--     GRANT EXECUTE ON SYS.DBMS_AQ TO LOAN_SYS;
--     EOF'
--     -- on production: the same two statements as SYS.
-- Reverse with: REVOKE EXECUTE ON SYS.DBMS_AQ FROM LOAN_SYS;
-- This block only CHECKS, and stops the file with a clear message when the grant is missing, so
-- file 2 does not fail later with the cryptic PLS-00201.
DECLARE n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_tab_privs
   WHERE table_name = 'DBMS_AQ' AND privilege = 'EXECUTE';
  IF n = 0 THEN
    RAISE_APPLICATION_ERROR(-20000,
      'LOAN_SYS has no direct EXECUTE on SYS.DBMS_AQ. As SYS in FREEPDB1 run: GRANT EXECUTE ON SYS.DBMS_AQ TO LOAN_SYS; then re-run this file.');
  END IF;
  DBMS_OUTPUT.PUT_LINE('EXECUTE ON SYS.DBMS_AQ granted directly — OK');
END;
/

-- ----------------------------------------------------------------------------
-- 1. Sequence — eventReference suffix
-- ----------------------------------------------------------------------------
DECLARE n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_sequences WHERE sequence_name = 'ACCOUNTING_EVENT_SEQ';
  IF n = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE ACCOUNTING_EVENT_SEQ START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('created ACCOUNTING_EVENT_SEQ');
  ELSE
    DBMS_OUTPUT.PUT_LINE('ACCOUNTING_EVENT_SEQ exists — skipped');
  END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2. Emission log — audit + replay source (plain table, no FK into legacy tables)
-- ----------------------------------------------------------------------------
DECLARE n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_tables WHERE table_name = 'ACCOUNTING_EVENT_LOG';
  IF n = 0 THEN
    EXECUTE IMMEDIATE q'[
      CREATE TABLE ACCOUNTING_EVENT_LOG (
        EVENT_LOG_PK      NUMBER          GENERATED ALWAYS AS IDENTITY,
        EVENT_REFERENCE   VARCHAR2(100)   NOT NULL,
        EVENT_TYPE_CODE   VARCHAR2(50)    NOT NULL,
        SOURCE_TABLE      VARCHAR2(30)    NOT NULL,
        SOURCE_PK         NUMBER          NOT NULL,
        SOURCE_ACTION     VARCHAR2(10)    NOT NULL,   -- INSERT | UPDATE | DELETE | REPLAY
        DOC_DATE          DATE            NOT NULL,
        BASE_AMOUNT       NUMBER(18,4)    NOT NULL,
        PAYLOAD           JSON            NOT NULL,
        ENQUEUED_FL       NUMBER(1)       DEFAULT 1 NOT NULL,   -- 0 = emit failed, payload kept for replay
        ENQUEUE_ERROR     VARCHAR2(4000),
        UNDO_FL           NUMBER(1)       DEFAULT 0 NOT NULL,   -- D-16: 1 = cancels an earlier emission (old value on UPDATE, DELETE)
        REVERSES_EVENT_REFERENCE VARCHAR2(100),                 -- D-16: that emission's reference, when one was found
        CREATED_DATE      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
        CREATED_BY        VARCHAR2(100)   DEFAULT SYS_CONTEXT('USERENV','SESSION_USER'),
        CONSTRAINT PK_ACCOUNTING_EVENT_LOG PRIMARY KEY (EVENT_LOG_PK),
        CONSTRAINT UQ_ACCOUNTING_EVENT_LOG_REF UNIQUE (EVENT_REFERENCE),
        CONSTRAINT CHK_ACCOUNTING_EVENT_LOG_ACTION CHECK (SOURCE_ACTION IN ('INSERT','UPDATE','DELETE','REPLAY'))
      )]';
    EXECUTE IMMEDIATE 'CREATE INDEX IX_ACCOUNTING_EVENT_LOG_SRC ON ACCOUNTING_EVENT_LOG (SOURCE_TABLE, SOURCE_PK)';
    EXECUTE IMMEDIATE 'CREATE INDEX IX_ACCOUNTING_EVENT_LOG_DATE ON ACCOUNTING_EVENT_LOG (DOC_DATE)';
    EXECUTE IMMEDIATE q'[COMMENT ON TABLE ACCOUNTING_EVENT_LOG IS 'Every accounting event emitted to ACCOUNTING_EVENT_Q — audit and replay source. Written by PKG_ACCOUNTING_EVENT only. Additive object of the FIN cutover (2026-09).']';
    DBMS_OUTPUT.PUT_LINE('created ACCOUNTING_EVENT_LOG');
  ELSE
    DBMS_OUTPUT.PUT_LINE('ACCOUNTING_EVENT_LOG exists — skipped');
  END IF;
  -- D-16 columns, added 2026-09-24 — converge a log table created by the earlier version of this file
  SELECT COUNT(*) INTO n FROM user_tab_columns WHERE table_name = 'ACCOUNTING_EVENT_LOG' AND column_name = 'UNDO_FL';
  IF n = 0 THEN
    EXECUTE IMMEDIATE 'ALTER TABLE ACCOUNTING_EVENT_LOG ADD (UNDO_FL NUMBER(1) DEFAULT 0 NOT NULL, REVERSES_EVENT_REFERENCE VARCHAR2(100))';
    DBMS_OUTPUT.PUT_LINE('added UNDO_FL / REVERSES_EVENT_REFERENCE to ACCOUNTING_EVENT_LOG');
  END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3. The queue — TxEventQ, JSON payload
-- ----------------------------------------------------------------------------
DECLARE n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_queues WHERE name = 'ACCOUNTING_EVENT_Q';
  IF n = 0 THEN
    -- max_retries is an argument of CREATE, not a later ALTER. Corrected 2026-09-23 after the
    -- first real run: DBMS_AQADM.ALTER_QUEUE raises ORA-24218 ("feature ALTER_QUEUE not supported
    -- for transactional event queues"), and because CREATE had already committed, that left the
    -- queue existing but never started (enqueue NO / dequeue NO). Do not reintroduce ALTER_QUEUE.
    DBMS_AQADM.CREATE_TRANSACTIONAL_EVENT_QUEUE(
      queue_name         => 'ACCOUNTING_EVENT_Q',
      queue_payload_type => 'JSON',
      multiple_consumers => FALSE,          -- one subscriber: the event consumer (plan §3.1)
      max_retries        => 5,              -- then the message goes to the exception queue (plan §2.3, §3.4)
      comment            => 'Canonical accounting events LOAN_SYS -> FIN. Business facts only; never an account code (plan §3.2).');

    -- retry_delay: NOT SETTABLE on a TxEventQ. GET_QUEUE_PARAMETER accepts only SHARD_NUM,
    -- KEY_BASED_ENQUEUE and STICKY_DEQUEUE; RETRY_DELAY / RETRY_INTERVAL / MAX_RETRIES all raise
    -- ORA-00904 "Unsupported param". The 60 s spacing the plan asks for is therefore the
    -- CONSUMER's responsibility (back-off between redelivery attempts), not the queue's.

    -- FIFO. A TxEventQ is sharded — this instance defaults to SHARD_NUM = 5 — and order is
    -- guaranteed only WITHIN a shard. The plan's per-document ordering (an update is a reversal
    -- plus a new entry, §3.5) is a correctness property, so collapse to a single shard: with one
    -- shard and one consumer the queue is globally FIFO, which is what §2.1 assumed all along.
    -- Volume (~4,800 journal-producing rows in 9 months) is nowhere near needing 5 shards.
    DBMS_AQADM.SET_QUEUE_PARAMETER('ACCOUNTING_EVENT_Q', 'SHARD_NUM', 1);

    DBMS_AQADM.START_QUEUE(queue_name => 'ACCOUNTING_EVENT_Q', enqueue => TRUE, dequeue => TRUE);
    DBMS_OUTPUT.PUT_LINE('created + started ACCOUNTING_EVENT_Q (TxEventQ, JSON, max_retries 5, 1 shard)');
  ELSE
    -- The queue exists. That is NOT the same as the queue being usable: CREATE commits on its
    -- own, so a failure anywhere later in this block (as happened on the first real run) leaves a
    -- queue that exists, has the wrong shard count and is not started. "Skip if present" would
    -- then never repair it. So converge the settings instead of assuming them.
    DBMS_AQADM.SET_QUEUE_PARAMETER('ACCOUNTING_EVENT_Q', 'SHARD_NUM', 1);
    -- START_QUEUE on an already-started queue raises ORA-24210 (second real run, 2026-09-24), so
    -- start only the side that is actually stopped.
    DECLARE v_enq VARCHAR2(10); v_deq VARCHAR2(10);
    BEGIN
      SELECT TRIM(enqueue_enabled), TRIM(dequeue_enabled) INTO v_enq, v_deq
        FROM user_queues WHERE name = 'ACCOUNTING_EVENT_Q';
      IF v_enq <> 'YES' OR v_deq <> 'YES' THEN
        DBMS_AQADM.START_QUEUE(queue_name => 'ACCOUNTING_EVENT_Q',
                               enqueue => (v_enq <> 'YES'), dequeue => (v_deq <> 'YES'));
        DBMS_OUTPUT.PUT_LINE('ACCOUNTING_EVENT_Q existed — started (enqueue was '||v_enq||', dequeue was '||v_deq||')');
      ELSE
        DBMS_OUTPUT.PUT_LINE('ACCOUNTING_EVENT_Q existed and is started — shard count reasserted');
      END IF;
    END;
  END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3b. Exception queue — without it a message past max_retries is PURGED (added 2026-09-24)
-- ----------------------------------------------------------------------------
-- Oracle 26 DBMS_AQADM: "After the retry limit has been exceeded, the message will be purged from
-- the queue", and "by default, no exception queue is created for TxEventQs". A rollback of a
-- dequeue counts as a retry (measured: attempts 0 → 1). So without this section, five rolled-back
-- deliveries of one message = a committed business fact whose event silently vanished. With it,
-- the message moves here, and fin-consumer sweeps it into its failure store (FAILED /
-- EXCEPTION_QUEUE) where it is visible and replayable. Dequeue-only by design.
DECLARE n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_queues WHERE name = 'ACCOUNTING_EVENT_Q_EXCPT';
  IF n = 0 THEN
    -- Parameter names read from ALL_ARGUMENTS on 23.26.2 (queue_name, exception_queue_name); the
    -- published reference's "teq_queue_name" is refused here with PLS-00306.
    DBMS_AQADM.CREATE_EQ_EXCEPTION_QUEUE(queue_name           => 'ACCOUNTING_EVENT_Q',
                                         exception_queue_name => 'ACCOUNTING_EVENT_Q_EXCPT');
    DBMS_OUTPUT.PUT_LINE('created exception queue ACCOUNTING_EVENT_Q_EXCPT');
  ELSE
    DBMS_OUTPUT.PUT_LINE('ACCOUNTING_EVENT_Q_EXCPT existed');
  END IF;
  -- converge: an exception queue is started for DEQUEUE only (enqueue is AQ's own job)
  DECLARE v_deq VARCHAR2(10);
  BEGIN
    SELECT TRIM(dequeue_enabled) INTO v_deq FROM user_queues WHERE name = 'ACCOUNTING_EVENT_Q_EXCPT';
    IF v_deq <> 'YES' THEN
      DBMS_AQADM.START_QUEUE(queue_name => 'ACCOUNTING_EVENT_Q_EXCPT', enqueue => FALSE, dequeue => TRUE);
      DBMS_OUTPUT.PUT_LINE('ACCOUNTING_EVENT_Q_EXCPT started for dequeue');
    END IF;
  END;
END;
/

-- FALLBACK — only if CREATE_TRANSACTIONAL_EVENT_QUEUE is unavailable on the target edition.
-- Same queue name, same enqueue call, same JMS consumer; sort_list is immutable after creation
-- (plan §2.3), so it is decided here: ENQ_TIME = FIFO.
--   BEGIN
--     DBMS_AQADM.CREATE_QUEUE_TABLE(queue_table => 'ACCOUNTING_EVENT_QT', queue_payload_type => 'JSON',
--                                   sort_list => 'ENQ_TIME', multiple_consumers => FALSE,
--                                   comment => 'Canonical accounting events LOAN_SYS -> FIN');
--     DBMS_AQADM.CREATE_QUEUE(queue_name => 'ACCOUNTING_EVENT_Q', queue_table => 'ACCOUNTING_EVENT_QT',
--                             max_retries => 5, retry_delay => 60);
--     DBMS_AQADM.START_QUEUE(queue_name => 'ACCOUNTING_EVENT_Q');
--   END;
--   /

-- ----------------------------------------------------------------------------
-- 4. Verification (read-only)
-- ----------------------------------------------------------------------------
SELECT name, queue_type, enqueue_enabled, dequeue_enabled, max_retries, retry_delay, queue_table
  FROM user_queues WHERE name LIKE 'ACCOUNTING_EVENT_Q%' OR name LIKE 'AQ$_ACCOUNTING%' ORDER BY name;
-- expected: ACCOUNTING_EVENT_Q NORMAL_QUEUE enq=YES deq=YES max_retries=5 retry_delay=0
--           ACCOUNTING_EVENT_Q_EXCPT EXCEPTION_QUEUE enq=NO deq=YES (section 3b)
--           (retry_delay is always 0 on a TxEventQ — see section 3; it is not a defect)
DECLARE v NUMBER; BEGIN
  DBMS_AQADM.GET_QUEUE_PARAMETER('ACCOUNTING_EVENT_Q', 'SHARD_NUM', v);
  DBMS_OUTPUT.PUT_LINE('SHARD_NUM = '||v||'   (expected 1 — anything else breaks global FIFO)');
END;
/
SELECT sequence_name FROM user_sequences WHERE sequence_name = 'ACCOUNTING_EVENT_SEQ';
SELECT COUNT(*) AS log_rows FROM ACCOUNTING_EVENT_LOG;   -- expected: 0 on first run

-- ============================================================================
-- ROLLBACK COMPANION — reverse order; the log table is kept unless you really mean it
-- ============================================================================
-- BEGIN DBMS_AQADM.STOP_QUEUE(queue_name => 'ACCOUNTING_EVENT_Q'); DBMS_AQADM.DROP_TRANSACTIONAL_EVENT_QUEUE(queue_name => 'ACCOUNTING_EVENT_Q'); END;
-- /
-- -- classic fallback instead: BEGIN DBMS_AQADM.STOP_QUEUE('ACCOUNTING_EVENT_Q'); DBMS_AQADM.DROP_QUEUE('ACCOUNTING_EVENT_Q'); DBMS_AQADM.DROP_QUEUE_TABLE('ACCOUNTING_EVENT_QT'); END;
-- DROP SEQUENCE ACCOUNTING_EVENT_SEQ;
-- -- DROP TABLE ACCOUNTING_EVENT_LOG PURGE;   -- deliberately commented twice: it is the audit trail
