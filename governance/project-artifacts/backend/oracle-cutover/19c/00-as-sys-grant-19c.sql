-- ============================================================================
-- 00-as-sys-grant-19c.sql — the ONE statement LOAN_SYS cannot run for itself     [19c file 0 of 4]
-- ============================================================================
-- Why: PKG_ACCOUNTING_EVENT (file 2) calls DBMS_AQ.ENQUEUE. Inside stored PL/SQL the DBA role is
-- disabled, so LOAN_SYS needs a DIRECT grant, and a user cannot grant to itself (ORA-01749).
-- Verified on production 2026-09-25: LOAN_SYS has DBA (role) but no direct EXECUTE on DBMS_AQ, and
-- DBMS_AQ is not granted to PUBLIC.
--
-- RUN ON THE PRODUCTION SERVER (as SYS, OS authentication inside the Oracle container):
--   docker ps --format '{{.Names}} {{.Image}}'            # find the Oracle 19c container
--   docker exec -i <container> bash -lc 'sqlplus -S / as sysdba' < 00-as-sys-grant-19c.sql
-- Additive, reversible: REVOKE EXECUTE ON SYS.DBMS_AQ FROM LOAN_SYS;
-- ============================================================================
WHENEVER SQLERROR EXIT FAILURE
ALTER SESSION SET CONTAINER = ORCLPDB;
GRANT EXECUTE ON SYS.DBMS_AQ TO LOAN_SYS;
SELECT grantee, privilege FROM dba_tab_privs WHERE owner = 'SYS' AND table_name = 'DBMS_AQ' AND grantee = 'LOAN_SYS';
EXIT
