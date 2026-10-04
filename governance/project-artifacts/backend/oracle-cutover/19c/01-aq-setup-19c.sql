-- ============================================================================
-- 01-aq-setup-19c.sql — accounting event queue, Oracle 19c port of oracle-aq-setup.sql   [19c file 1 of 4]
-- ============================================================================
-- Prepared 2026-09-25 for PRODUCTION: LOAN_SYS@89.117.37.75:1892/orclpdb — Oracle 19c EE 19.3,
-- compatible 19.0.0, max_string_size STANDARD, aq_tm_processes 1, LOAN_SYS holds DBA (role only).
--
-- WHY A PORT (the 26ai original cannot run here)
--   TxEventQ (CREATE_TRANSACTIONAL_EVENT_QUEUE), CREATE_EQ_EXCEPTION_QUEUE, the JSON column type and
--   the JSON queue payload are all 21c+. On 19c:
--     payload   object type ACCOUNTING_EVENT_MSG_T(PAYLOAD CLOB) — the same JSON text, wrapped
--     queue     classic AQ, queue table ACCOUNTING_EVENT_QT, sort ENQ_TIME (= FIFO), single consumer
--     retries   max_retries 5, retry_delay 0 — a rolled-back message is READY again at once and, being
--               the oldest, is dequeued first: FIFO survives a retry (a delay would let later
--               messages overtake it). Back-off stays the consumer's job, as on 26ai.
--     exception ACCOUNTING_EVENT_Q_EXCPT, an EXCEPTION_QUEUE in the same queue table; the package
--               names it on every message (D-20), so the name and the consumer sweep are unchanged.
--     log       ACCOUNTING_EVENT_LOG.PAYLOAD is CLOB CHECK (PAYLOAD IS JSON)
--   Object names are identical to the 26ai build, so the runbooks, FLOW-REFERENCE and the consumer's
--   queue names do not change.
--
-- PREREQUISITE (SYS, once): GRANT EXECUTE ON SYS.DBMS_AQ TO LOAN_SYS;   — inside the PDB.
--   The DBA role is disabled inside stored PL/SQL, so the package (file 2) needs a DIRECT grant, and
--   LOAN_SYS cannot grant to itself (ORA-01749). Section 0 stops this file when it is missing.
--
-- IDEMPOTENT: every step checks first. ADDITIVE: touches no existing object. ROLLBACK at the end.
-- ============================================================================

SET ECHO ON
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT FAILURE

-- 0. direct EXECUTE on DBMS_AQ ------------------------------------------------
DECLARE n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_tab_privs WHERE table_name = 'DBMS_AQ' AND privilege = 'EXECUTE';
  IF n = 0 THEN
    RAISE_APPLICATION_ERROR(-20000,
      'LOAN_SYS has no direct EXECUTE on SYS.DBMS_AQ. As SYS in the PDB run: GRANT EXECUTE ON SYS.DBMS_AQ TO LOAN_SYS; then re-run.');
  END IF;
  DBMS_OUTPUT.PUT_LINE('EXECUTE ON SYS.DBMS_AQ granted directly - OK');
END;
/

-- 1. sequence -----------------------------------------------------------------
DECLARE n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_sequences WHERE sequence_name = 'ACCOUNTING_EVENT_SEQ';
  IF n = 0 THEN
    EXECUTE IMMEDIATE 'CREATE SEQUENCE ACCOUNTING_EVENT_SEQ START WITH 1 INCREMENT BY 1 CACHE 100 NOCYCLE';
    DBMS_OUTPUT.PUT_LINE('created ACCOUNTING_EVENT_SEQ');
  END IF;
END;
/

-- 2. emission log (CLOB IS JSON on 19c) ----------------------------------------
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
        SOURCE_ACTION     VARCHAR2(10)    NOT NULL,
        DOC_DATE          DATE            NOT NULL,
        BASE_AMOUNT       NUMBER(18,4)    NOT NULL,
        PAYLOAD           CLOB            NOT NULL,
        ENQUEUED_FL       NUMBER(1)       DEFAULT 1 NOT NULL,
        ENQUEUE_ERROR     VARCHAR2(4000),
        UNDO_FL           NUMBER(1)       DEFAULT 0 NOT NULL,
        REVERSES_EVENT_REFERENCE VARCHAR2(100),
        CREATED_DATE      TIMESTAMP       DEFAULT SYSTIMESTAMP NOT NULL,
        CREATED_BY        VARCHAR2(100)   DEFAULT SYS_CONTEXT('USERENV','SESSION_USER'),
        CONSTRAINT PK_ACCOUNTING_EVENT_LOG PRIMARY KEY (EVENT_LOG_PK),
        CONSTRAINT UQ_ACCOUNTING_EVENT_LOG_REF UNIQUE (EVENT_REFERENCE),
        CONSTRAINT CHK_ACCOUNTING_EVENT_LOG_ACTION CHECK (SOURCE_ACTION IN ('INSERT','UPDATE','DELETE','REPLAY')),
        CONSTRAINT CHK_ACCOUNTING_EVENT_LOG_JSON CHECK (PAYLOAD IS JSON)
      )]';
    EXECUTE IMMEDIATE 'CREATE INDEX IX_ACCOUNTING_EVENT_LOG_SRC ON ACCOUNTING_EVENT_LOG (SOURCE_TABLE, SOURCE_PK)';
    EXECUTE IMMEDIATE 'CREATE INDEX IX_ACCOUNTING_EVENT_LOG_DATE ON ACCOUNTING_EVENT_LOG (DOC_DATE)';
    EXECUTE IMMEDIATE q'[COMMENT ON TABLE ACCOUNTING_EVENT_LOG IS 'Every accounting event emitted to ACCOUNTING_EVENT_Q - audit and replay source. Written by PKG_ACCOUNTING_EVENT only. FIN cutover (2026-09).']';
    DBMS_OUTPUT.PUT_LINE('created ACCOUNTING_EVENT_LOG');
  END IF;
END;
/

-- 3. payload type ---------------------------------------------------------------
DECLARE n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_types WHERE type_name = 'ACCOUNTING_EVENT_MSG_T';
  IF n = 0 THEN
    EXECUTE IMMEDIATE 'CREATE TYPE ACCOUNTING_EVENT_MSG_T AS OBJECT (PAYLOAD CLOB)';
    DBMS_OUTPUT.PUT_LINE('created type ACCOUNTING_EVENT_MSG_T');
  END IF;
END;
/

-- 4. queue table + queue + exception queue --------------------------------------
DECLARE n NUMBER;
BEGIN
  SELECT COUNT(*) INTO n FROM user_queue_tables WHERE queue_table = 'ACCOUNTING_EVENT_QT';
  IF n = 0 THEN
    DBMS_AQADM.CREATE_QUEUE_TABLE(
      queue_table        => 'ACCOUNTING_EVENT_QT',
      queue_payload_type => 'ACCOUNTING_EVENT_MSG_T',
      sort_list          => 'ENQ_TIME',          -- FIFO; immutable after creation
      multiple_consumers => FALSE,
      comment            => 'Canonical accounting events LOAN_SYS -> FIN (19c classic AQ)');
    DBMS_OUTPUT.PUT_LINE('created queue table ACCOUNTING_EVENT_QT');
  END IF;

  SELECT COUNT(*) INTO n FROM user_queues WHERE name = 'ACCOUNTING_EVENT_Q';
  IF n = 0 THEN
    DBMS_AQADM.CREATE_QUEUE(queue_name => 'ACCOUNTING_EVENT_Q', queue_table => 'ACCOUNTING_EVENT_QT',
                            max_retries => 5, retry_delay => 0,
                            comment => 'Business facts only; never an account code');
    DBMS_OUTPUT.PUT_LINE('created ACCOUNTING_EVENT_Q');
  END IF;

  SELECT COUNT(*) INTO n FROM user_queues WHERE name = 'ACCOUNTING_EVENT_Q_EXCPT';
  IF n = 0 THEN
    DBMS_AQADM.CREATE_QUEUE(queue_name => 'ACCOUNTING_EVENT_Q_EXCPT', queue_table => 'ACCOUNTING_EVENT_QT',
                            queue_type => DBMS_AQADM.EXCEPTION_QUEUE,
                            comment => 'Messages past max_retries - swept by fin-consumer');
    DBMS_OUTPUT.PUT_LINE('created ACCOUNTING_EVENT_Q_EXCPT');
  END IF;
END;
/

-- 5. start (converge: only the side that is stopped) ----------------------------
DECLARE v_enq VARCHAR2(10); v_deq VARCHAR2(10);
BEGIN
  SELECT TRIM(enqueue_enabled), TRIM(dequeue_enabled) INTO v_enq, v_deq FROM user_queues WHERE name = 'ACCOUNTING_EVENT_Q';
  IF v_enq <> 'YES' OR v_deq <> 'YES' THEN
    DBMS_AQADM.START_QUEUE('ACCOUNTING_EVENT_Q', enqueue => TRUE, dequeue => TRUE);
    DBMS_OUTPUT.PUT_LINE('started ACCOUNTING_EVENT_Q');
  END IF;
  SELECT TRIM(dequeue_enabled) INTO v_deq FROM user_queues WHERE name = 'ACCOUNTING_EVENT_Q_EXCPT';
  IF v_deq <> 'YES' THEN
    DBMS_AQADM.START_QUEUE('ACCOUNTING_EVENT_Q_EXCPT', enqueue => FALSE, dequeue => TRUE);
    DBMS_OUTPUT.PUT_LINE('started ACCOUNTING_EVENT_Q_EXCPT (dequeue only)');
  END IF;
END;
/

-- 6. verification (read-only) -----------------------------------------------------
SELECT name, queue_type, TRIM(enqueue_enabled) enq, TRIM(dequeue_enabled) deq, max_retries, retry_delay, queue_table
  FROM user_queues WHERE queue_table = 'ACCOUNTING_EVENT_QT' ORDER BY name;
-- expected: ACCOUNTING_EVENT_Q NORMAL_QUEUE YES YES 5 0 · ACCOUNTING_EVENT_Q_EXCPT EXCEPTION_QUEUE NO YES
--           · AQ$_ACCOUNTING_EVENT_QT_E (AQ's default exception queue, unused)

-- ============================================================================
-- ROLLBACK (reverse order; the log is the audit trail — kept)
-- BEGIN
--   DBMS_AQADM.STOP_QUEUE('ACCOUNTING_EVENT_Q_EXCPT'); DBMS_AQADM.STOP_QUEUE('ACCOUNTING_EVENT_Q');
--   DBMS_AQADM.DROP_QUEUE_TABLE(queue_table => 'ACCOUNTING_EVENT_QT', force => TRUE);
-- END;
-- /
-- DROP TYPE ACCOUNTING_EVENT_MSG_T; DROP SEQUENCE ACCOUNTING_EVENT_SEQ;
-- -- DROP TABLE ACCOUNTING_EVENT_LOG PURGE;
-- ============================================================================
