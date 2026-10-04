-- ============================================================================
-- 03-triggers-19c.sql — TRG_AE_* event triggers, 19c/production build   [19c file 3 of 4]
-- ============================================================================
-- 19c BUILD (2026-09-25): trigger bodies IDENTICAL to oracle-event-emit-triggers.sql. The difference
-- is how they go live on production tables: each is CREATED DISABLED, and the block after SHOW
-- ERRORS enables all seven only when the package and every trigger compiled VALID. Every column
-- referenced was verified to exist on production (LOAN_SYS@orclpdb) on 2026-09-25.
-- ---------------------------------------------------------------------------- original header:
-- Prepared 2026-09-23. Requires files 1 and 2. Seven AFTER ROW triggers, TRG_AE_<TABLE>, one per
-- business table that the retired accounting triggers used to post from. They read the row that
-- was just saved and call PKG_ACCOUNTING_EVENT.emit — nothing else. No journal row, no account,
-- no balance is touched. Every trigger is additive and independent of the retired ones; the KEEP
-- list of legacy-accounting-stop.sql is untouched.
--
-- BRANCH → EVENT MAP (findings §4; the legacy *_FL vocabulary decoded from SYSTEM_TANSACTION_DT)
--   LOAN_PAYMENT.LOAN_PAYMENT_FL  7 INSTALLMENT_PAYMENT_RECEIVED · 32 INVESTOR_SUPPORT_OR_DRAWING ·
--     66 INVESTOR_SALES_INVOICE · 58 LAWYER_ADVANCE · 59 INVESTOR_FEE_2_5 · 49 INVESTOR_FEE ·
--     55 EXECUTION_ACTION_FEE · 56 BAEETHA_FEE · 57 CIVIL_FEE (negative is the normal sign for
--     32/55/56/57/58/59/66 — measured, findings D-14) ·
--     8 SUPPLIER_PAYMENT_MADE · 130 OWNER_DRAWING (both dead in data, D-07: emitted anyway so a
--     revival fails LOUDLY at FIN with FIN-404-NO-ACTIVE-RULE instead of vanishing)
--   CONTRACT.CONTRACT_FL 12 CONTRACT_CREATED (CASH_PRICE) · 13 CONTRACT_SETTLEMENT (DISCOUNT ≠ 0);
--     13 → 12 revert emits CONTRACT_SETTLEMENT_REVERSED (legacy DELETED the settlement entry)
--   COMPLAINTS.COMPLAINT_FL → 1 COMPLAINT_SETTLEMENT (DISCOUNT); 1 → 0 emits _REVERSED
--   COMPLAINT_DT.COMPLAINT_DT_FL 43 COURT_INSTALLMENT_PAYMENT · 44 LAWYER_FEE_PAYMENT (VALUE)
--   INVOICE_IMPORT  PURCHASE_INVOICE — three amounts: grossAmount CASH_PRICE, paidAmount PAYMENT,
--     remainingAmount REMAINING (legacy's three-line entry)
--   EXPENSE_TYPE_DT.EXPENSE_TYPE_DT_FK 64 REVENUE_RECEIVED · 65 EXPENSE_PAID · 131 CUSTODY_RECEIVED ·
--     132 CUSTODY_PAID_OUT (EXPENSE_AMOUNT; EXPENSE_TYPE_FK → fields.EXPENSE_TYPE_CODE;
--     org/branch from COST_CENTER_FK / SUB_COST_CENTER_FK exactly as the legacy trigger did)
--   TRANSFER_SAFE.TRANSFER_SAFE_TYPE_FK 25 SAFE_TRANSFER_TO_SAFE · 24 SAFE_TRANSFER_TO_BANK
--     (AMOUNT; org/branch from TO_* as the legacy trigger did)
--
-- UPDATE / DELETE SEMANTICS (plan §3.5; FIN has no update path — a change is reversal + re-post)
--   INSERT               → emit(new)
--   UPDATE of the amount → emit(old, reverse) then emit(new)           (two references, FIFO)
--   UPDATE of the status → leaving a posting state emits reverse(old); entering one emits new
--   DELETE               → emit(old, reverse)
--   Other updates (notes, dates, party) emit nothing — legacy did not re-post on those either.
--
-- HOW TO RUN   sqlplus LOAN_SYS/...@//host:1521/FREEPDB1 @oracle-event-emit-triggers.sql
-- SHADOW MODE  create these while the legacy *_Daliy_TR triggers are still ENABLED: both paths
--              run, the consumer's output is compared, nothing is switched (plan §5 Phase 3–4).
-- ROLLBACK     the DISABLE / DROP statements at the end, any time, no data impact.
-- ============================================================================

SET ECHO ON
SET SERVEROUTPUT ON SIZE UNLIMITED
WHENEVER SQLERROR EXIT FAILURE

-- ----------------------------------------------------------------------------
-- 1. LOAN_PAYMENT — 9 live branches + 2 dead ones
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AE_LOAN_PAYMENT
  -- D-17: the legacy trigger fired on EVERY update and rewrote the journal row (date, org, branch
  -- included). Firing only on amount/type/payment would silently leave FIN in the old period or
  -- branch after a date or organisation edit, so those columns are part of the change set too.
  AFTER INSERT OR UPDATE OF LOAN_PAYMENT_VALUE, LOAN_PAYMENT_FL, PAYMENT_TYPE_FK, LOAN_PAYMENT_DATE, ORGANIZATION_FK, ORGANIZATION_SUB_FK OR DELETE ON LOAN_PAYMENT
  FOR EACH ROW
  DISABLE   -- 19c/prod: created disabled; the block after SHOW ERRORS enables all 7 only if all compile VALID
DECLARE
  FUNCTION type_of(p_fl NUMBER) RETURN VARCHAR2 IS
  BEGIN
    RETURN CASE p_fl
      WHEN 7   THEN 'INSTALLMENT_PAYMENT_RECEIVED'
      WHEN 32  THEN 'INVESTOR_SUPPORT_OR_DRAWING'
      WHEN 66  THEN 'INVESTOR_SALES_INVOICE'
      WHEN 58  THEN 'LAWYER_ADVANCE'
      WHEN 59  THEN 'INVESTOR_FEE_2_5'
      WHEN 49  THEN 'INVESTOR_FEE'
      WHEN 55  THEN 'EXECUTION_ACTION_FEE'
      WHEN 56  THEN 'BAEETHA_FEE'
      WHEN 57  THEN 'CIVIL_FEE'
      WHEN 8   THEN 'SUPPLIER_PAYMENT_MADE'
      WHEN 130 THEN 'OWNER_DRAWING'
      ELSE NULL END;
  END;
  -- Measured on 2026 data (findings D-14): negative is the normal sign for 32 (469/505), 55, 56,
  -- 57, 58, 59, 66 (100 % negative); positive for 7 and 49 (100 % positive). The FIN rules are
  -- seeded in the orientation the books show for the normal sign, so _REVERSED is the exception.
  FUNCTION normal_sign(p_fl NUMBER) RETURN NUMBER IS
  BEGIN RETURN CASE WHEN p_fl IN (32, 55, 56, 57, 58, 59, 66) THEN -1 ELSE 1 END; END;
  FUNCTION note_of(p_fl NUMBER) RETURN VARCHAR2 IS
  BEGIN
    RETURN CASE p_fl
      WHEN 7   THEN 'قيد الى لسداد عميل'
      WHEN 8   THEN 'قيد الى لسداد مورد'
      WHEN 32  THEN 'قيد آلي ناتج عن دعم / مسحوبات مستثمر'
      WHEN 66  THEN 'قيد آلي ناتج عن فاتورة مبيعات للمستثمر'
      WHEN 58  THEN 'قيد آلي ناتج عن مقدم المحامي'
      WHEN 59  THEN 'قيد آلي ناتج عن سداد رسوم الشكوي'
      WHEN 49  THEN 'قيد آلي ناتج عن رسوم المستثمر'
      WHEN 55  THEN 'قيد آلى عن اضافة اجراء تنفيذي'
      WHEN 56  THEN 'قيد آلى عن اضافة اجراء بعيثة'
      WHEN 57  THEN 'قيد آلى عن اضافة رسوم مدنية'
      WHEN 130 THEN 'قيد آلي ناتج عن دعم / مسحوبات صاحب الشركة'
      ELSE NULL END;
  END;
  PROCEDURE send(p_action VARCHAR2, p_reverse BOOLEAN,
                 p_pk NUMBER, p_fl NUMBER, p_value NUMBER, p_date DATE, p_org NUMBER, p_sub NUMBER, p_pt NUMBER,
                 p_cust NUMBER, p_supp NUMBER, p_inv NUMBER, p_contract NUMBER, p_complaint NUMBER, p_note VARCHAR2) IS
    v_type VARCHAR2(50) := type_of(p_fl);
  BEGIN
    IF v_type IS NULL THEN RETURN; END IF;   -- a flag the legacy accounting never posted for
    PKG_ACCOUNTING_EVENT.emit(
      p_source_table => 'LOAN_PAYMENT', p_source_pk => p_pk, p_source_action => p_action,
      p_event_type => v_type, p_normal_sign => normal_sign(p_fl), p_reverse => p_reverse,
      p_doc_date => p_date, p_base_amount => p_value,
      p_org_fk => p_org, p_sub_fk => p_sub, p_payment_type_fk => p_pt,
      p_party_type => CASE WHEN p_supp IS NOT NULL THEN 'SUPPLIER' WHEN p_cust IS NOT NULL THEN 'CUSTOMER' WHEN p_inv IS NOT NULL THEN 'INVESTOR' END,
      p_party_pk   => COALESCE(p_supp, p_cust, p_inv),
      p_contract_pk => p_contract, p_investor_pk => p_inv, p_complaint_pk => p_complaint,
      p_description_ar => note_of(p_fl) || ' ' || p_note,
      p_description_en => v_type || ' ' || p_note);
  END;
BEGIN
  IF INSERTING THEN
    send('INSERT', FALSE, :NEW.LOAN_PAYMENT_PK, :NEW.LOAN_PAYMENT_FL, :NEW.LOAN_PAYMENT_VALUE, :NEW.LOAN_PAYMENT_DATE,
         :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK, :NEW.PAYMENT_TYPE_FK, :NEW.CUSTOMERS_FK, :NEW.SUPPLIERS_FK,
         :NEW.INVESTOR_FK, :NEW.CONTRACT_FK, :NEW.COMPLAINT_FK, :NEW.NOTE);
  ELSIF UPDATING THEN
    IF NVL(:OLD.LOAN_PAYMENT_VALUE, 0) <> NVL(:NEW.LOAN_PAYMENT_VALUE, 0)
       OR NVL(:OLD.LOAN_PAYMENT_FL, -1) <> NVL(:NEW.LOAN_PAYMENT_FL, -1)
       OR NVL(:OLD.PAYMENT_TYPE_FK, -1) <> NVL(:NEW.PAYMENT_TYPE_FK, -1)
       OR NVL(:OLD.LOAN_PAYMENT_DATE, DATE '1900-01-01') <> NVL(:NEW.LOAN_PAYMENT_DATE, DATE '1900-01-01')
       OR NVL(:OLD.ORGANIZATION_FK, -1) <> NVL(:NEW.ORGANIZATION_FK, -1)
       OR NVL(:OLD.ORGANIZATION_SUB_FK, -1) <> NVL(:NEW.ORGANIZATION_SUB_FK, -1) THEN
      send('UPDATE', TRUE,  :OLD.LOAN_PAYMENT_PK, :OLD.LOAN_PAYMENT_FL, :OLD.LOAN_PAYMENT_VALUE, :OLD.LOAN_PAYMENT_DATE,
           :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.PAYMENT_TYPE_FK, :OLD.CUSTOMERS_FK, :OLD.SUPPLIERS_FK,
           :OLD.INVESTOR_FK, :OLD.CONTRACT_FK, :OLD.COMPLAINT_FK, :OLD.NOTE);
      send('UPDATE', FALSE, :NEW.LOAN_PAYMENT_PK, :NEW.LOAN_PAYMENT_FL, :NEW.LOAN_PAYMENT_VALUE, :NEW.LOAN_PAYMENT_DATE,
           :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK, :NEW.PAYMENT_TYPE_FK, :NEW.CUSTOMERS_FK, :NEW.SUPPLIERS_FK,
           :NEW.INVESTOR_FK, :NEW.CONTRACT_FK, :NEW.COMPLAINT_FK, :NEW.NOTE);
    END IF;
  ELSIF DELETING THEN
    send('DELETE', TRUE, :OLD.LOAN_PAYMENT_PK, :OLD.LOAN_PAYMENT_FL, :OLD.LOAN_PAYMENT_VALUE, :OLD.LOAN_PAYMENT_DATE,
         :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.PAYMENT_TYPE_FK, :OLD.CUSTOMERS_FK, :OLD.SUPPLIERS_FK,
         :OLD.INVESTOR_FK, :OLD.CONTRACT_FK, :OLD.COMPLAINT_FK, :OLD.NOTE);
  END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 2. CONTRACT — creation (FL 12, CASH_PRICE) and settlement (FL 13, DISCOUNT)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AE_CONTRACT
  AFTER INSERT OR UPDATE OF CASH_PRICE, CASH_TOTAL_CONTRACT, DISCOUNT, CONTRACT_FL, CONTRACT_DATE, QUITTANCE_DATE, ORGANIZATION_FK, ORGANIZATION_SUB_FK OR DELETE ON CONTRACT   -- D-17
  FOR EACH ROW
  DISABLE   -- 19c/prod: created disabled; the block after SHOW ERRORS enables all 7 only if all compile VALID
DECLARE
  PROCEDURE created(p_action VARCHAR2, p_reverse BOOLEAN, p_pk NUMBER, p_amount NUMBER, p_date DATE,
                    p_org NUMBER, p_sub NUMBER, p_cust NUMBER, p_inv NUMBER, p_no VARCHAR2, p_note VARCHAR2,
                    p_total NUMBER) IS
    -- D-23: the contract's own amounts, as business facts, so a rule may split the entry (an
    -- installment sale: total contract vs cash price). "amount" stays = CASH_PRICE for the seeded
    -- 2-line rule. PAYMENT is NOT sent: its meaning is unconfirmed (0 on every 2026 contract).
    v_amounts JSON_OBJECT_T := JSON_OBJECT_T();
  BEGIN
    v_amounts.put('amount',        NVL(p_amount, 0));
    v_amounts.put('cashPrice',     NVL(p_amount, 0));
    v_amounts.put('totalContract', NVL(p_total, 0));    -- CASH_TOTAL_CONTRACT: the instalment total
    PKG_ACCOUNTING_EVENT.emit('CONTRACT', p_pk, p_action, 'CONTRACT_CREATED', 1, p_reverse,
      p_doc_date => NVL(p_date, SYSDATE), p_base_amount => p_amount, p_amounts => v_amounts,
      p_org_fk => p_org, p_sub_fk => p_sub,
      p_party_type => 'CUSTOMER', p_party_pk => p_cust, p_contract_pk => p_pk, p_investor_pk => p_inv,
      p_contract_no => p_no,   -- D-21: never let the package SELECT the mutating CONTRACT row
      p_description_ar => 'قيد الى ناتج عن عقد ' || p_note || ' عقد رقم ' || p_no,
      p_description_en => 'Contract created ' || p_no);
  END;
  PROCEDURE settled(p_action VARCHAR2, p_reverse BOOLEAN, p_pk NUMBER, p_amount NUMBER, p_date DATE,
                    p_org NUMBER, p_sub NUMBER, p_cust NUMBER, p_inv NUMBER, p_no VARCHAR2, p_note VARCHAR2) IS
  BEGIN
    PKG_ACCOUNTING_EVENT.emit('CONTRACT', p_pk, p_action, 'CONTRACT_SETTLEMENT', 1, p_reverse,
      p_doc_date => NVL(p_date, SYSDATE), p_base_amount => p_amount, p_org_fk => p_org, p_sub_fk => p_sub,
      p_party_type => 'CUSTOMER', p_party_pk => p_cust, p_contract_pk => p_pk, p_investor_pk => p_inv,
      p_contract_no => p_no,   -- D-21: never let the package SELECT the mutating CONTRACT row
      p_description_ar => 'قيد الى ناتج عن مخالصه عقد ' || p_note || ' عقد رقم ' || p_no,
      p_description_en => 'Contract settlement ' || p_no);
  END;
BEGIN
  IF INSERTING THEN
    IF :NEW.CONTRACT_FL = 12 THEN
      created('INSERT', FALSE, :NEW.CONTRACT_PK, :NEW.CASH_PRICE, :NEW.CONTRACT_DATE, :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK,
              :NEW.CUSTOMER_FK, :NEW.INVESTORS_FK, :NEW.CONTRACT_AUTO_PK, :NEW.NOTE, :NEW.CASH_TOTAL_CONTRACT);
    END IF;
  ELSIF UPDATING THEN
    -- creation amount changed while the contract is live (legacy re-wrote the entry in place)
    IF :OLD.CONTRACT_FL = 12 AND :NEW.CONTRACT_FL = 12
       AND (NVL(:OLD.CASH_PRICE, 0) <> NVL(:NEW.CASH_PRICE, 0)
            OR NVL(:OLD.CASH_TOTAL_CONTRACT, 0) <> NVL(:NEW.CASH_TOTAL_CONTRACT, 0)   -- D-23
            OR NVL(:OLD.CONTRACT_DATE, DATE '1900-01-01') <> NVL(:NEW.CONTRACT_DATE, DATE '1900-01-01')
            OR NVL(:OLD.ORGANIZATION_FK, -1) <> NVL(:NEW.ORGANIZATION_FK, -1)
            OR NVL(:OLD.ORGANIZATION_SUB_FK, -1) <> NVL(:NEW.ORGANIZATION_SUB_FK, -1)) THEN   -- D-17
      created('UPDATE', TRUE,  :OLD.CONTRACT_PK, :OLD.CASH_PRICE, :OLD.CONTRACT_DATE, :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.CUSTOMER_FK, :OLD.INVESTORS_FK, :OLD.CONTRACT_AUTO_PK, :OLD.NOTE, :OLD.CASH_TOTAL_CONTRACT);
      created('UPDATE', FALSE, :NEW.CONTRACT_PK, :NEW.CASH_PRICE, :NEW.CONTRACT_DATE, :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK, :NEW.CUSTOMER_FK, :NEW.INVESTORS_FK, :NEW.CONTRACT_AUTO_PK, :NEW.NOTE, :NEW.CASH_TOTAL_CONTRACT);
    END IF;
    -- entering settlement (legacy posted 236/32 for DISCOUNT ≠ 0)
    IF NVL(:OLD.CONTRACT_FL, -1) <> 13 AND :NEW.CONTRACT_FL = 13 AND NVL(:NEW.DISCOUNT, 0) <> 0 THEN
      settled('UPDATE', FALSE, :NEW.CONTRACT_PK, :NEW.DISCOUNT, :NEW.QUITTANCE_DATE, :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK, :NEW.CUSTOMER_FK, :NEW.INVESTORS_FK, :NEW.CONTRACT_AUTO_PK, :NEW.NOTE);
    END IF;
    -- settlement amount changed while settled
    IF :OLD.CONTRACT_FL = 13 AND :NEW.CONTRACT_FL = 13
       AND (NVL(:OLD.DISCOUNT, 0) <> NVL(:NEW.DISCOUNT, 0)
            OR NVL(:OLD.QUITTANCE_DATE, DATE '1900-01-01') <> NVL(:NEW.QUITTANCE_DATE, DATE '1900-01-01')
            OR NVL(:OLD.ORGANIZATION_FK, -1) <> NVL(:NEW.ORGANIZATION_FK, -1)
            OR NVL(:OLD.ORGANIZATION_SUB_FK, -1) <> NVL(:NEW.ORGANIZATION_SUB_FK, -1)) THEN   -- D-17
      settled('UPDATE', TRUE,  :OLD.CONTRACT_PK, :OLD.DISCOUNT, :OLD.QUITTANCE_DATE, :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.CUSTOMER_FK, :OLD.INVESTORS_FK, :OLD.CONTRACT_AUTO_PK, :OLD.NOTE);
      settled('UPDATE', FALSE, :NEW.CONTRACT_PK, :NEW.DISCOUNT, :NEW.QUITTANCE_DATE, :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK, :NEW.CUSTOMER_FK, :NEW.INVESTORS_FK, :NEW.CONTRACT_AUTO_PK, :NEW.NOTE);
    END IF;
    -- leaving settlement: 13 → 12 (legacy DELETED the settlement entry; here it is reversed)
    IF :OLD.CONTRACT_FL = 13 AND NVL(:NEW.CONTRACT_FL, -1) <> 13 AND NVL(:OLD.DISCOUNT, 0) <> 0 THEN
      settled('UPDATE', TRUE, :OLD.CONTRACT_PK, :OLD.DISCOUNT, :OLD.QUITTANCE_DATE, :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.CUSTOMER_FK, :OLD.INVESTORS_FK, :OLD.CONTRACT_AUTO_PK, :OLD.NOTE);
    END IF;
  ELSIF DELETING THEN
    IF :OLD.CONTRACT_FL IN (12, 13, 14, 45) AND NVL(:OLD.CASH_PRICE, 0) <> 0 THEN
      created('DELETE', TRUE, :OLD.CONTRACT_PK, :OLD.CASH_PRICE, :OLD.CONTRACT_DATE, :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.CUSTOMER_FK, :OLD.INVESTORS_FK, :OLD.CONTRACT_AUTO_PK, :OLD.NOTE, :OLD.CASH_TOTAL_CONTRACT);
    END IF;
    IF :OLD.CONTRACT_FL = 13 AND NVL(:OLD.DISCOUNT, 0) <> 0 THEN
      settled('DELETE', TRUE, :OLD.CONTRACT_PK, :OLD.DISCOUNT, :OLD.QUITTANCE_DATE, :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.CUSTOMER_FK, :OLD.INVESTORS_FK, :OLD.CONTRACT_AUTO_PK, :OLD.NOTE);
    END IF;
  END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 3. COMPLAINTS — complaint settlement (FL 1, DISCOUNT)
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AE_COMPLAINTS
  AFTER UPDATE OF COMPLAINT_FL, DISCOUNT, QUITTANCE_DATE, ORGANIZATION_FK, SUB_ORGANIZATION_FK OR DELETE ON COMPLAINTS   -- D-17
  FOR EACH ROW
  DISABLE   -- 19c/prod: created disabled; the block after SHOW ERRORS enables all 7 only if all compile VALID
DECLARE
  PROCEDURE settled(p_action VARCHAR2, p_reverse BOOLEAN, p_pk NUMBER, p_amount NUMBER, p_date DATE,
                    p_org NUMBER, p_sub NUMBER, p_cust NUMBER, p_inv NUMBER, p_contract NUMBER, p_pt NUMBER, p_note VARCHAR2,
                    p_no VARCHAR2) IS
  BEGIN
    PKG_ACCOUNTING_EVENT.emit('COMPLAINTS', p_pk, p_action, 'COMPLAINT_SETTLEMENT', 1, p_reverse,
      p_doc_date => NVL(p_date, SYSDATE), p_base_amount => p_amount, p_org_fk => p_org, p_sub_fk => p_sub,
      p_payment_type_fk => p_pt, p_party_type => 'CUSTOMER', p_party_pk => p_cust,
      p_contract_pk => p_contract, p_investor_pk => p_inv, p_complaint_pk => p_pk,
      p_complaint_no => p_no,  -- D-21: never let the package SELECT the mutating COMPLAINTS row
      p_description_ar => 'قيد الى ناتج عن مخالصه شكوى ' || p_note,
      p_description_en => 'Complaint settlement');
  END;
BEGIN
  IF UPDATING THEN
    IF NVL(:OLD.COMPLAINT_FL, 0) <> 1 AND :NEW.COMPLAINT_FL = 1 AND NVL(:NEW.DISCOUNT, 0) <> 0 THEN
      settled('UPDATE', FALSE, :NEW.COMPLAINT_ID, :NEW.DISCOUNT, :NEW.QUITTANCE_DATE, :NEW.ORGANIZATION_FK, :NEW.SUB_ORGANIZATION_FK, :NEW.CUSTOMER_FK, :NEW.INVESTORS_FK, :NEW.CONTRACT_FK, :NEW.PAYMENT_TYPE_FK, :NEW.NOTS, TO_CHAR(:NEW.COMPLAINT_NUMBER));
    ELSIF :OLD.COMPLAINT_FL = 1 AND :NEW.COMPLAINT_FL = 1
          AND (NVL(:OLD.DISCOUNT, 0) <> NVL(:NEW.DISCOUNT, 0)
               OR NVL(:OLD.QUITTANCE_DATE, DATE '1900-01-01') <> NVL(:NEW.QUITTANCE_DATE, DATE '1900-01-01')
               OR NVL(:OLD.ORGANIZATION_FK, -1) <> NVL(:NEW.ORGANIZATION_FK, -1)
               OR NVL(:OLD.SUB_ORGANIZATION_FK, -1) <> NVL(:NEW.SUB_ORGANIZATION_FK, -1)) THEN   -- D-17
      settled('UPDATE', TRUE,  :OLD.COMPLAINT_ID, :OLD.DISCOUNT, :OLD.QUITTANCE_DATE, :OLD.ORGANIZATION_FK, :OLD.SUB_ORGANIZATION_FK, :OLD.CUSTOMER_FK, :OLD.INVESTORS_FK, :OLD.CONTRACT_FK, :OLD.PAYMENT_TYPE_FK, :OLD.NOTS, TO_CHAR(:OLD.COMPLAINT_NUMBER));
      settled('UPDATE', FALSE, :NEW.COMPLAINT_ID, :NEW.DISCOUNT, :NEW.QUITTANCE_DATE, :NEW.ORGANIZATION_FK, :NEW.SUB_ORGANIZATION_FK, :NEW.CUSTOMER_FK, :NEW.INVESTORS_FK, :NEW.CONTRACT_FK, :NEW.PAYMENT_TYPE_FK, :NEW.NOTS, TO_CHAR(:NEW.COMPLAINT_NUMBER));
    ELSIF :OLD.COMPLAINT_FL = 1 AND NVL(:NEW.COMPLAINT_FL, 0) <> 1 AND NVL(:OLD.DISCOUNT, 0) <> 0 THEN
      settled('UPDATE', TRUE, :OLD.COMPLAINT_ID, :OLD.DISCOUNT, :OLD.QUITTANCE_DATE, :OLD.ORGANIZATION_FK, :OLD.SUB_ORGANIZATION_FK, :OLD.CUSTOMER_FK, :OLD.INVESTORS_FK, :OLD.CONTRACT_FK, :OLD.PAYMENT_TYPE_FK, :OLD.NOTS, TO_CHAR(:OLD.COMPLAINT_NUMBER));
    END IF;
  ELSIF DELETING THEN
    IF :OLD.COMPLAINT_FL = 1 AND NVL(:OLD.DISCOUNT, 0) <> 0 THEN
      settled('DELETE', TRUE, :OLD.COMPLAINT_ID, :OLD.DISCOUNT, :OLD.QUITTANCE_DATE, :OLD.ORGANIZATION_FK, :OLD.SUB_ORGANIZATION_FK, :OLD.CUSTOMER_FK, :OLD.INVESTORS_FK, :OLD.CONTRACT_FK, :OLD.PAYMENT_TYPE_FK, :OLD.NOTS, TO_CHAR(:OLD.COMPLAINT_NUMBER));
    END IF;
  END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 4. COMPLAINT_DT — court installment (43) and lawyer fee (44) payments
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AE_COMPLAINT_DT
  AFTER INSERT OR UPDATE OF VALUE, COMPLAINT_DT_FL, PAYMENT_TYPE_FK, COMPLAINT_DT_DATE, ORGANIZATION_FK, ORGANIZATION_SUB_FK OR DELETE ON COMPLAINT_DT   -- D-17
  FOR EACH ROW
  DISABLE   -- 19c/prod: created disabled; the block after SHOW ERRORS enables all 7 only if all compile VALID
DECLARE
  PROCEDURE send(p_action VARCHAR2, p_reverse BOOLEAN, p_pk NUMBER, p_fl NUMBER, p_amount NUMBER, p_date DATE,
                 p_org NUMBER, p_sub NUMBER, p_pt NUMBER, p_contract NUMBER, p_complaint NUMBER, p_note VARCHAR2) IS
    v_type VARCHAR2(50) := CASE p_fl WHEN 43 THEN 'COURT_INSTALLMENT_PAYMENT' WHEN 44 THEN 'LAWYER_FEE_PAYMENT' END;
  BEGIN
    IF v_type IS NULL THEN RETURN; END IF;
    PKG_ACCOUNTING_EVENT.emit('COMPLAINT_DT', p_pk, p_action, v_type, 1, p_reverse,
      p_doc_date => NVL(p_date, SYSDATE), p_base_amount => p_amount, p_org_fk => p_org, p_sub_fk => p_sub,
      p_payment_type_fk => p_pt, p_contract_pk => p_contract, p_complaint_pk => p_complaint,
      p_description_ar => CASE p_fl WHEN 43 THEN 'قيد الى ناتج عن سداد شكوى ' ELSE 'قيد الى ناتج عن سداد دفعات محامى ' END || p_note,
      p_description_en => v_type);
  END;
BEGIN
  IF INSERTING THEN
    send('INSERT', FALSE, :NEW.COMPLAINT_DT_PK, :NEW.COMPLAINT_DT_FL, :NEW.VALUE, :NEW.COMPLAINT_DT_DATE, :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK, :NEW.PAYMENT_TYPE_FK, :NEW.CONTRACT_FK, :NEW.COMPLAINT_FK, :NEW.NOTS);
  ELSIF UPDATING THEN
    IF NVL(:OLD.VALUE, 0) <> NVL(:NEW.VALUE, 0) OR NVL(:OLD.COMPLAINT_DT_FL, -1) <> NVL(:NEW.COMPLAINT_DT_FL, -1) OR NVL(:OLD.PAYMENT_TYPE_FK, -1) <> NVL(:NEW.PAYMENT_TYPE_FK, -1)
       OR NVL(:OLD.COMPLAINT_DT_DATE, DATE '1900-01-01') <> NVL(:NEW.COMPLAINT_DT_DATE, DATE '1900-01-01')
       OR NVL(:OLD.ORGANIZATION_FK, -1) <> NVL(:NEW.ORGANIZATION_FK, -1) OR NVL(:OLD.ORGANIZATION_SUB_FK, -1) <> NVL(:NEW.ORGANIZATION_SUB_FK, -1) THEN   -- D-17
      send('UPDATE', TRUE,  :OLD.COMPLAINT_DT_PK, :OLD.COMPLAINT_DT_FL, :OLD.VALUE, :OLD.COMPLAINT_DT_DATE, :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.PAYMENT_TYPE_FK, :OLD.CONTRACT_FK, :OLD.COMPLAINT_FK, :OLD.NOTS);
      send('UPDATE', FALSE, :NEW.COMPLAINT_DT_PK, :NEW.COMPLAINT_DT_FL, :NEW.VALUE, :NEW.COMPLAINT_DT_DATE, :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK, :NEW.PAYMENT_TYPE_FK, :NEW.CONTRACT_FK, :NEW.COMPLAINT_FK, :NEW.NOTS);
    END IF;
  ELSIF DELETING THEN
    send('DELETE', TRUE, :OLD.COMPLAINT_DT_PK, :OLD.COMPLAINT_DT_FL, :OLD.VALUE, :OLD.COMPLAINT_DT_DATE, :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.PAYMENT_TYPE_FK, :OLD.CONTRACT_FK, :OLD.COMPLAINT_FK, :OLD.NOTS);
  END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 5. INVOICE_IMPORT — purchase invoice, three amounts
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AE_INVOICE_IMPORT
  AFTER INSERT OR UPDATE OF CASH_PRICE, PAYMENT, REMAINING, PAYMENT_TYPE_FK, INV_IMPORT_DATE, ORGANIZATION_FK, ORGANIZATION_SUB_FK OR DELETE ON INVOICE_IMPORT   -- D-17
  FOR EACH ROW
  DISABLE   -- 19c/prod: created disabled; the block after SHOW ERRORS enables all 7 only if all compile VALID
DECLARE
  PROCEDURE send(p_action VARCHAR2, p_reverse BOOLEAN, p_pk NUMBER, p_gross NUMBER, p_paid NUMBER, p_remaining NUMBER, p_date DATE,
                 p_org NUMBER, p_sub NUMBER, p_pt NUMBER, p_supp NUMBER, p_inv NUMBER, p_note VARCHAR2) IS
    v_amounts JSON_OBJECT_T := JSON_OBJECT_T();
  BEGIN
    v_amounts.put('grossAmount',     NVL(p_gross, 0));
    v_amounts.put('paidAmount',      NVL(p_paid, 0));
    v_amounts.put('remainingAmount', NVL(p_remaining, 0));
    PKG_ACCOUNTING_EVENT.emit('INVOICE_IMPORT', p_pk, p_action, 'PURCHASE_INVOICE', 1, p_reverse,
      p_doc_date => NVL(p_date, SYSDATE), p_base_amount => p_gross, p_amounts => v_amounts,
      p_org_fk => p_org, p_sub_fk => p_sub, p_payment_type_fk => p_pt,
      p_party_type => 'SUPPLIER', p_party_pk => p_supp, p_investor_pk => p_inv,
      p_description_ar => 'قيد الى ناتج عن فاتورة مشتريات ' || p_note, p_description_en => 'Purchase invoice');
  END;
BEGIN
  IF INSERTING THEN
    send('INSERT', FALSE, :NEW.INVOICE_IMPORT_PK, :NEW.CASH_PRICE, :NEW.PAYMENT, :NEW.REMAINING, :NEW.INV_IMPORT_DATE, :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK, :NEW.PAYMENT_TYPE_FK, :NEW.SUPPLIER_FK, :NEW.INVESTORS_FK, :NEW.NOTE);
  ELSIF UPDATING THEN
    IF NVL(:OLD.CASH_PRICE,0) <> NVL(:NEW.CASH_PRICE,0) OR NVL(:OLD.PAYMENT,0) <> NVL(:NEW.PAYMENT,0) OR NVL(:OLD.REMAINING,0) <> NVL(:NEW.REMAINING,0) OR NVL(:OLD.PAYMENT_TYPE_FK,-1) <> NVL(:NEW.PAYMENT_TYPE_FK,-1)
       OR NVL(:OLD.INV_IMPORT_DATE, DATE '1900-01-01') <> NVL(:NEW.INV_IMPORT_DATE, DATE '1900-01-01')
       OR NVL(:OLD.ORGANIZATION_FK,-1) <> NVL(:NEW.ORGANIZATION_FK,-1) OR NVL(:OLD.ORGANIZATION_SUB_FK,-1) <> NVL(:NEW.ORGANIZATION_SUB_FK,-1) THEN   -- D-17
      send('UPDATE', TRUE,  :OLD.INVOICE_IMPORT_PK, :OLD.CASH_PRICE, :OLD.PAYMENT, :OLD.REMAINING, :OLD.INV_IMPORT_DATE, :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.PAYMENT_TYPE_FK, :OLD.SUPPLIER_FK, :OLD.INVESTORS_FK, :OLD.NOTE);
      send('UPDATE', FALSE, :NEW.INVOICE_IMPORT_PK, :NEW.CASH_PRICE, :NEW.PAYMENT, :NEW.REMAINING, :NEW.INV_IMPORT_DATE, :NEW.ORGANIZATION_FK, :NEW.ORGANIZATION_SUB_FK, :NEW.PAYMENT_TYPE_FK, :NEW.SUPPLIER_FK, :NEW.INVESTORS_FK, :NEW.NOTE);
    END IF;
  ELSIF DELETING THEN
    send('DELETE', TRUE, :OLD.INVOICE_IMPORT_PK, :OLD.CASH_PRICE, :OLD.PAYMENT, :OLD.REMAINING, :OLD.INV_IMPORT_DATE, :OLD.ORGANIZATION_FK, :OLD.ORGANIZATION_SUB_FK, :OLD.PAYMENT_TYPE_FK, :OLD.SUPPLIER_FK, :OLD.INVESTORS_FK, :OLD.NOTE);
  END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 6. EXPENSE_TYPE_DT — expense / revenue / custody, by category, account by expense type
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AE_EXPENSE_TYPE_DT
  AFTER INSERT OR UPDATE OF EXPENSE_AMOUNT, EXPENSE_TYPE_FK, EXPENSE_TYPE_DT_FK, PAYMENT_TYPE_FK, EXPENSE_DATE, COST_CENTER_FK, SUB_COST_CENTER_FK OR DELETE ON EXPENSE_TYPE_DT   -- D-17
  FOR EACH ROW
  DISABLE   -- 19c/prod: created disabled; the block after SHOW ERRORS enables all 7 only if all compile VALID
DECLARE
  FUNCTION type_of(p_cat NUMBER) RETURN VARCHAR2 IS
  BEGIN RETURN CASE p_cat WHEN 64 THEN 'REVENUE_RECEIVED' WHEN 65 THEN 'EXPENSE_PAID' WHEN 131 THEN 'CUSTODY_RECEIVED' WHEN 132 THEN 'CUSTODY_PAID_OUT' END; END;
  PROCEDURE send(p_action VARCHAR2, p_reverse BOOLEAN, p_pk NUMBER, p_cat NUMBER, p_type_id NUMBER, p_amount NUMBER, p_date DATE,
                 p_cc NUMBER, p_scc NUMBER, p_pt NUMBER, p_contract NUMBER, p_complaint NUMBER, p_complaint_dt NUMBER, p_emp NUMBER, p_name VARCHAR2, p_note VARCHAR2) IS
    v_type VARCHAR2(50) := type_of(p_cat);
  BEGIN
    IF v_type IS NULL THEN RETURN; END IF;
    IF PKG_ACCOUNTING_EVENT.c_skip_complaint_mirrors AND p_complaint_dt IS NOT NULL THEN RETURN; END IF;  -- OQ-14
    PKG_ACCOUNTING_EVENT.emit('EXPENSE_TYPE_DT', p_pk, p_action, v_type, 1, p_reverse,
      p_doc_date => NVL(p_date, SYSDATE), p_base_amount => p_amount,
      p_org_fk => p_cc, p_sub_fk => p_scc,                       -- legacy: ORGANIZATION_FK = COST_CENTER_FK, SUB = SUB_COST_CENTER_FK
      p_payment_type_fk => p_pt, p_expense_type_id => p_type_id,
      p_party_type => CASE WHEN p_emp IS NOT NULL THEN 'EMPLOYEE' END, p_party_pk => p_emp,
      p_contract_pk => p_contract, p_complaint_pk => p_complaint,
      p_description_ar => CASE p_cat WHEN 64 THEN 'قيد الى ناتج عن إيراد (' WHEN 65 THEN 'قيد الى ناتج عن مصروف (' WHEN 131 THEN 'قيد الى ناتج عن معاملة واردة (' ELSE 'قيد الى ناتج عن معاملة صادرة (' END || NVL(p_name, '') || ') ' || p_note,
      p_description_en => v_type || ' (' || NVL(p_name, '') || ')');
  END;
BEGIN
  IF INSERTING THEN
    send('INSERT', FALSE, :NEW.EXPENSE_TYPE_DT_ID, :NEW.EXPENSE_TYPE_DT_FK, :NEW.EXPENSE_TYPE_FK, :NEW.EXPENSE_AMOUNT, :NEW.EXPENSE_DATE, :NEW.COST_CENTER_FK, :NEW.SUB_COST_CENTER_FK, :NEW.PAYMENT_TYPE_FK, :NEW.CONTRACT_FK, :NEW.COMPLAINT_ID, :NEW.COMPLAINT_DT_FK, :NEW.EMPLOYEE_FK, :NEW.EXPENSE_NAME, :NEW.NOTS);
  ELSIF UPDATING THEN
    IF NVL(:OLD.EXPENSE_AMOUNT,0) <> NVL(:NEW.EXPENSE_AMOUNT,0) OR NVL(:OLD.EXPENSE_TYPE_FK,-1) <> NVL(:NEW.EXPENSE_TYPE_FK,-1) OR NVL(:OLD.EXPENSE_TYPE_DT_FK,-1) <> NVL(:NEW.EXPENSE_TYPE_DT_FK,-1) OR NVL(:OLD.PAYMENT_TYPE_FK,-1) <> NVL(:NEW.PAYMENT_TYPE_FK,-1)
       OR NVL(:OLD.EXPENSE_DATE, DATE '1900-01-01') <> NVL(:NEW.EXPENSE_DATE, DATE '1900-01-01')
       OR NVL(:OLD.COST_CENTER_FK,-1) <> NVL(:NEW.COST_CENTER_FK,-1) OR NVL(:OLD.SUB_COST_CENTER_FK,-1) <> NVL(:NEW.SUB_COST_CENTER_FK,-1) THEN   -- D-17
      send('UPDATE', TRUE,  :OLD.EXPENSE_TYPE_DT_ID, :OLD.EXPENSE_TYPE_DT_FK, :OLD.EXPENSE_TYPE_FK, :OLD.EXPENSE_AMOUNT, :OLD.EXPENSE_DATE, :OLD.COST_CENTER_FK, :OLD.SUB_COST_CENTER_FK, :OLD.PAYMENT_TYPE_FK, :OLD.CONTRACT_FK, :OLD.COMPLAINT_ID, :OLD.COMPLAINT_DT_FK, :OLD.EMPLOYEE_FK, :OLD.EXPENSE_NAME, :OLD.NOTS);
      send('UPDATE', FALSE, :NEW.EXPENSE_TYPE_DT_ID, :NEW.EXPENSE_TYPE_DT_FK, :NEW.EXPENSE_TYPE_FK, :NEW.EXPENSE_AMOUNT, :NEW.EXPENSE_DATE, :NEW.COST_CENTER_FK, :NEW.SUB_COST_CENTER_FK, :NEW.PAYMENT_TYPE_FK, :NEW.CONTRACT_FK, :NEW.COMPLAINT_ID, :NEW.COMPLAINT_DT_FK, :NEW.EMPLOYEE_FK, :NEW.EXPENSE_NAME, :NEW.NOTS);
    END IF;
  ELSIF DELETING THEN
    send('DELETE', TRUE, :OLD.EXPENSE_TYPE_DT_ID, :OLD.EXPENSE_TYPE_DT_FK, :OLD.EXPENSE_TYPE_FK, :OLD.EXPENSE_AMOUNT, :OLD.EXPENSE_DATE, :OLD.COST_CENTER_FK, :OLD.SUB_COST_CENTER_FK, :OLD.PAYMENT_TYPE_FK, :OLD.CONTRACT_FK, :OLD.COMPLAINT_ID, :OLD.COMPLAINT_DT_FK, :OLD.EMPLOYEE_FK, :OLD.EXPENSE_NAME, :OLD.NOTS);
  END IF;
END;
/

-- ----------------------------------------------------------------------------
-- 7. TRANSFER_SAFE — bank ↔ safe transfers
-- ----------------------------------------------------------------------------
CREATE OR REPLACE TRIGGER TRG_AE_TRANSFER_SAFE
  AFTER INSERT OR UPDATE OF AMOUNT, TRANSFER_SAFE_TYPE_FK, TRANSFER_SAFE_DATE, TO_ORGANIZATION_FK, TO_ORGANIZATION_SUB_FK OR DELETE ON TRANSFER_SAFE   -- D-17
  FOR EACH ROW
  DISABLE   -- 19c/prod: created disabled; the block after SHOW ERRORS enables all 7 only if all compile VALID
DECLARE
  PROCEDURE send(p_action VARCHAR2, p_reverse BOOLEAN, p_pk NUMBER, p_type_fk NUMBER, p_amount NUMBER, p_date DATE, p_org NUMBER, p_sub NUMBER) IS
    v_type VARCHAR2(50) := CASE p_type_fk WHEN 25 THEN 'SAFE_TRANSFER_TO_SAFE' WHEN 24 THEN 'SAFE_TRANSFER_TO_BANK' END;
  BEGIN
    IF v_type IS NULL THEN RETURN; END IF;
    PKG_ACCOUNTING_EVENT.emit('TRANSFER_SAFE', p_pk, p_action, v_type, 1, p_reverse,
      p_doc_date => NVL(p_date, SYSDATE), p_base_amount => p_amount,
      p_org_fk => p_org, p_sub_fk => p_sub,                          -- legacy: TO_ORGANIZATION_FK / TO_ORGANIZATION_SUB_FK
      p_description_ar => CASE p_type_fk WHEN 25 THEN 'قيد آلي ناتج عن تحويل من حساب الى خزنة' ELSE 'قيد آلي ناتج عن تحويل من خزنة الى حساب' END,
      p_description_en => v_type);
  END;
BEGIN
  IF INSERTING THEN
    send('INSERT', FALSE, :NEW.TRANSFER_SAFE_PK, :NEW.TRANSFER_SAFE_TYPE_FK, :NEW.AMOUNT, :NEW.TRANSFER_SAFE_DATE, :NEW.TO_ORGANIZATION_FK, :NEW.TO_ORGANIZATION_SUB_FK);
  ELSIF UPDATING THEN
    IF NVL(:OLD.AMOUNT,0) <> NVL(:NEW.AMOUNT,0) OR NVL(:OLD.TRANSFER_SAFE_TYPE_FK,-1) <> NVL(:NEW.TRANSFER_SAFE_TYPE_FK,-1)
       OR NVL(:OLD.TRANSFER_SAFE_DATE, DATE '1900-01-01') <> NVL(:NEW.TRANSFER_SAFE_DATE, DATE '1900-01-01')
       OR NVL(:OLD.TO_ORGANIZATION_FK,-1) <> NVL(:NEW.TO_ORGANIZATION_FK,-1) OR NVL(:OLD.TO_ORGANIZATION_SUB_FK,-1) <> NVL(:NEW.TO_ORGANIZATION_SUB_FK,-1) THEN   -- D-17
      send('UPDATE', TRUE,  :OLD.TRANSFER_SAFE_PK, :OLD.TRANSFER_SAFE_TYPE_FK, :OLD.AMOUNT, :OLD.TRANSFER_SAFE_DATE, :OLD.TO_ORGANIZATION_FK, :OLD.TO_ORGANIZATION_SUB_FK);
      send('UPDATE', FALSE, :NEW.TRANSFER_SAFE_PK, :NEW.TRANSFER_SAFE_TYPE_FK, :NEW.AMOUNT, :NEW.TRANSFER_SAFE_DATE, :NEW.TO_ORGANIZATION_FK, :NEW.TO_ORGANIZATION_SUB_FK);
    END IF;
  ELSIF DELETING THEN
    send('DELETE', TRUE, :OLD.TRANSFER_SAFE_PK, :OLD.TRANSFER_SAFE_TYPE_FK, :OLD.AMOUNT, :OLD.TRANSFER_SAFE_DATE, :OLD.TO_ORGANIZATION_FK, :OLD.TO_ORGANIZATION_SUB_FK);
  END IF;
END;
/

SHOW ERRORS

-- ----------------------------------------------------------------------------
-- ENABLE — only when the package and all seven triggers are VALID. An ENABLED trigger that does not
-- compile fails EVERY insert/update/delete on its table (ORA-04098): on live data that stops the
-- business. So nothing is enabled unless everything is clean.
-- ----------------------------------------------------------------------------
DECLARE n_bad NUMBER; n_all NUMBER;
BEGIN
  SELECT COUNT(*) INTO n_all FROM user_objects
   WHERE object_type = 'TRIGGER' AND object_name LIKE 'TRG\_AE\_%' ESCAPE '\';
  SELECT COUNT(*) INTO n_bad FROM user_objects
   WHERE (object_name = 'PKG_ACCOUNTING_EVENT' OR (object_type = 'TRIGGER' AND object_name LIKE 'TRG\_AE\_%' ESCAPE '\'))
     AND status <> 'VALID';
  IF n_all <> 7 OR n_bad > 0 THEN
    RAISE_APPLICATION_ERROR(-20002, 'NOT ENABLED: ' || n_all || ' TRG_AE_* triggers, ' || n_bad
      || ' invalid objects. Nothing was switched on. Fix, then re-run this file.');
  END IF;
  FOR r IN (SELECT trigger_name FROM user_triggers WHERE trigger_name LIKE 'TRG\_AE\_%' ESCAPE '\') LOOP
    EXECUTE IMMEDIATE 'ALTER TRIGGER ' || r.trigger_name || ' ENABLE';
    DBMS_OUTPUT.PUT_LINE('enabled ' || r.trigger_name);
  END LOOP;
END;
/

-- ----------------------------------------------------------------------------
-- Verification (read-only)
-- ----------------------------------------------------------------------------
SELECT trigger_name, table_name, status FROM user_triggers WHERE trigger_name LIKE 'TRG_AE_%' ORDER BY 1;
-- expected: 7 rows, all ENABLED
SELECT object_name, status FROM user_objects WHERE object_name LIKE 'TRG_AE_%' AND status <> 'VALID';
-- expected: no rows
-- after the first business save: SELECT event_reference, event_type_code, enqueued_fl, enqueue_error FROM ACCOUNTING_EVENT_LOG ORDER BY event_log_pk DESC FETCH FIRST 10 ROWS ONLY;

-- ============================================================================
-- ROLLBACK COMPANION — pause (keeps definitions) or remove (drops)
-- ============================================================================
-- ALTER TRIGGER TRG_AE_LOAN_PAYMENT    DISABLE;   ALTER TRIGGER TRG_AE_CONTRACT      DISABLE;
-- ALTER TRIGGER TRG_AE_COMPLAINTS      DISABLE;   ALTER TRIGGER TRG_AE_COMPLAINT_DT  DISABLE;
-- ALTER TRIGGER TRG_AE_INVOICE_IMPORT  DISABLE;   ALTER TRIGGER TRG_AE_EXPENSE_TYPE_DT DISABLE;
-- ALTER TRIGGER TRG_AE_TRANSFER_SAFE   DISABLE;
-- -- or, to remove: DROP TRIGGER TRG_AE_<name>; for each of the seven, then DROP PACKAGE PKG_ACCOUNTING_EVENT;
