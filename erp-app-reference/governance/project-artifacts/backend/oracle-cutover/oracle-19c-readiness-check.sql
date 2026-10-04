-- ============================================================================
-- oracle-19c-readiness-check.sql — READ-ONLY inspection of the LIVE Oracle before the cutover port
-- ============================================================================
-- Prepared 2026-09-25. The cutover files 1–3 and fin-consumer were built and tested on 26ai
-- (23.26); production is 19c. This script only SELECTs (plus one anonymous block that builds a
-- JSON value in memory) — it creates, alters, grants and deletes NOTHING. Safe on live data.
--
-- HOW TO RUN (as SYS, in the container that holds the live database):
--   docker ps                                         # note the Oracle container name
--   docker cp oracle-19c-readiness-check.sql <container>:/tmp/check.sql
--   docker exec -i <container> bash -lc "sqlplus -S / as sysdba @/tmp/check.sql" > readiness.txt
-- Then paste readiness.txt back. Section A runs at the root; section B switches into the PDB that
-- holds LOAN_SYS automatically (or stays put on a non-CDB).
-- ============================================================================
SET PAGESIZE 200 LINESIZE 220 FEEDBACK OFF VERIFY OFF TRIMSPOOL ON SERVEROUTPUT ON
COLUMN name FORMAT A30
COLUMN value FORMAT A40
COLUMN banner_full FORMAT A100
PROMPT ===== A1. version / edition
SELECT banner_full FROM v$version;
SELECT name, value FROM v$parameter WHERE name IN ('compatible', 'aq_tm_processes', 'job_queue_processes', 'nls_characterset', 'max_string_size');
SELECT value AS db_charset FROM nls_database_parameters WHERE parameter = 'NLS_CHARACTERSET';
PROMPT ===== A2. CDB / PDBs
SELECT name, cdb, open_mode, log_mode FROM v$database;
SELECT con_id, name, open_mode FROM v$pdbs;

PROMPT ===== B0. switching into the container that owns LOAN_SYS (if CDB)
DECLARE v_cdb VARCHAR2(3); v_pdb VARCHAR2(128);
BEGIN
  SELECT cdb INTO v_cdb FROM v$database;
  IF v_cdb = 'YES' THEN
    SELECT p.name INTO v_pdb FROM cdb_users u JOIN v$pdbs p ON p.con_id = u.con_id
     WHERE u.username = 'LOAN_SYS' AND ROWNUM = 1;
    EXECUTE IMMEDIATE 'ALTER SESSION SET CONTAINER = "' || v_pdb || '"';
    DBMS_OUTPUT.PUT_LINE('LOAN_SYS lives in PDB: ' || v_pdb);
  ELSE
    DBMS_OUTPUT.PUT_LINE('non-CDB database');
  END IF;
EXCEPTION WHEN NO_DATA_FOUND THEN DBMS_OUTPUT.PUT_LINE('LOAN_SYS NOT FOUND in any PDB');
END;
/
-- re-enable output: the DBMS_OUTPUT buffer does not survive ALTER SESSION SET CONTAINER
SET SERVEROUTPUT ON
SELECT SYS_CONTEXT('USERENV', 'CON_NAME') AS current_container, SYS_CONTEXT('USERENV', 'SERVICE_NAME') AS service FROM dual;

PROMPT ===== B1. accounts
SELECT username, account_status, default_tablespace, TO_CHAR(created, 'YYYY-MM-DD') created FROM dba_users WHERE username IN ('LOAN_SYS', 'FIN_CONSUMER');
PROMPT ===== B2. LOAN_SYS roles and AQ privileges (direct grants matter for stored PL/SQL)
SELECT granted_role, admin_option FROM dba_role_privs WHERE grantee = 'LOAN_SYS' ORDER BY 1;
SELECT owner || '.' || table_name obj, privilege FROM dba_tab_privs WHERE grantee = 'LOAN_SYS' AND table_name IN ('DBMS_AQ', 'DBMS_AQADM', 'DBMS_LOCK') ORDER BY 1;
SELECT privilege FROM dba_sys_privs WHERE grantee = 'LOAN_SYS' AND (privilege LIKE '%QUEUE%' OR privilege LIKE 'CREATE %') ORDER BY 1;
PROMPT ===== B3. does anything of the cutover already exist?
SELECT object_type, object_name, status FROM dba_objects
 WHERE owner = 'LOAN_SYS' AND (object_name LIKE 'ACCOUNTING_EVENT%' OR object_name LIKE 'AQ$%ACCOUNTING%'
       OR object_name IN ('PKG_ACCOUNTING_EVENT', 'FINC_QUEUE_DEPTH') OR object_name LIKE 'TRG_AE\_%' ESCAPE '\') ORDER BY 1, 2;
SELECT owner, name, queue_type, enqueue_enabled, dequeue_enabled FROM dba_queues WHERE owner = 'LOAN_SYS';
PROMPT ===== B4. triggers on the 7 business tables (legacy accounting ones must stay ENABLED until shadow run passes)
SELECT table_name, trigger_name, status, triggering_event FROM dba_triggers
 WHERE owner = 'LOAN_SYS' AND table_name IN ('LOAN_PAYMENT', 'CONTRACT', 'COMPLAINTS', 'COMPLAINT_DT', 'INVOICE_IMPORT', 'EXPENSE_TYPE_DT', 'TRANSFER_SAFE')
 ORDER BY 1, 2;
PROMPT ===== B5. invalid objects already present in LOAN_SYS (baseline, so we never blame the cutover for them)
SELECT object_type, object_name FROM dba_objects WHERE owner = 'LOAN_SYS' AND status <> 'VALID' ORDER BY 1, 2;
PROMPT ===== B6. volume and activity (2026) — sizes the replay and the maintenance window
SELECT 'LOAN_PAYMENT' t, COUNT(*) n, TO_CHAR(MAX(loan_payment_date), 'YYYY-MM-DD') last_doc FROM loan_sys.loan_payment WHERE loan_payment_date >= DATE '2026-01-01'
UNION ALL SELECT 'CONTRACT', COUNT(*), TO_CHAR(MAX(contract_date), 'YYYY-MM-DD') FROM loan_sys.contract WHERE contract_date >= DATE '2026-01-01'
UNION ALL SELECT 'COMPLAINT_DT', COUNT(*), TO_CHAR(MAX(complaint_dt_date), 'YYYY-MM-DD') FROM loan_sys.complaint_dt WHERE complaint_dt_date >= DATE '2026-01-01'
UNION ALL SELECT 'INVOICE_IMPORT', COUNT(*), TO_CHAR(MAX(inv_import_date), 'YYYY-MM-DD') FROM loan_sys.invoice_import WHERE inv_import_date >= DATE '2026-01-01'
UNION ALL SELECT 'EXPENSE_TYPE_DT', COUNT(*), TO_CHAR(MAX(expense_date), 'YYYY-MM-DD') FROM loan_sys.expense_type_dt WHERE expense_date >= DATE '2026-01-01'
UNION ALL SELECT 'TRANSFER_SAFE', COUNT(*), TO_CHAR(MAX(transfer_safe_date), 'YYYY-MM-DD') FROM loan_sys.transfer_safe WHERE transfer_safe_date >= DATE '2026-01-01';
SELECT COUNT(*) sessions_as_loan_sys FROM v$session WHERE username = 'LOAN_SYS';
PROMPT ===== B7. space for the new objects
SELECT tablespace_name, ROUND(SUM(bytes) / 1048576) free_mb FROM dba_free_space
 WHERE tablespace_name = (SELECT default_tablespace FROM dba_users WHERE username = 'LOAN_SYS') GROUP BY tablespace_name;
PROMPT ===== B8. JSON capability of THIS database (in memory only, nothing stored)
DECLARE o JSON_OBJECT_T := JSON_OBJECT_T(); k JSON_KEY_LIST; c CLOB;
BEGIN
  o.put('a', 1); o.put('b', 'x'); k := o.get_keys; c := o.to_clob();
  DBMS_OUTPUT.PUT_LINE('JSON_OBJECT_T / JSON_KEY_LIST / to_clob: OK -> ' || DBMS_LOB.SUBSTR(c, 100, 1));
EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.PUT_LINE('JSON_OBJECT_T: ' || SQLERRM);
END;
/
SELECT JSON_SERIALIZE('{"a":1}') AS json_serialize_ok FROM dual;
SELECT CASE WHEN '{"a":1}' IS JSON THEN 'IS JSON: OK' END AS is_json FROM dual;
PROMPT ===== B9. AQ packages present
SELECT object_name, status FROM dba_objects WHERE owner = 'SYS' AND object_name IN ('DBMS_AQ', 'DBMS_AQADM') AND object_type = 'PACKAGE';
SELECT COUNT(*) AS txeventq_procedure_present FROM dba_procedures WHERE owner = 'SYS' AND object_name = 'DBMS_AQADM' AND procedure_name = 'CREATE_TRANSACTIONAL_EVENT_QUEUE';
PROMPT ===== END (nothing was changed)
EXIT
