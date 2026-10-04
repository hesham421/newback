-- ============================================================
-- V40 — Finance / General Ledger (FIN) — chart of accounts restructure (IFRS presentation)
-- ============================================================
-- Why: the chart seeded by V38 is the legacy LOAN_SYS chart read 1:1 — a contracting-company template
--   with equity and contra-assets filed under liabilities, revenue nested inside expenses, and none of
--   the accounts an instalment-trading business needs (instalment receivables, deferred instalment
--   income, cost of sales, expected credit losses, investors' current accounts). Full review and the
--   owner's approval (2026-09-26): governance/project-artifacts/fin-chart-of-accounts-review.md.
-- When: production FIN has ZERO journal entries at this point (events wait in the Oracle queue), so
--   this is configuration, not a reclassification of posted history.
--
-- How (idempotent, one transaction; section 6 RAISEs and Flyway rolls everything back):
--   1. seed_coa — the target chart. A row with old_code REUSES that existing account (same
--      account_pk, so FIN_ACCOUNT_MAPPING and any journal line keep pointing at it) and recodes /
--      renames / re-types / re-parents it. Rows are matched by OLD CODE, never by pk: pks differ
--      between environments.
--   2. rule lines that name an account by code (CONSTANT) are moved to the new codes BEFORE the recode.
--   3. reused accounts are rewritten; missing ones are inserted; parents are linked by code.
--   4. two rule corrections the review calls for (COMPLAINT_SETTLEMENT, LAWYER_FEE_PAYMENT — see §4).
--   5. every legacy account NOT in the target chart and referenced by nothing is deleted (leaves
--      first); anything still referenced (test data in a dev DB) is deactivated under an archive node.
--   6. verification.
-- Codes: class (1) / group (2) / account (4) / sub-account (6), strictly prefix-consistent.
-- Branch, department, investor and contract are DIMENSIONS, never accounts.
-- Never edit this file once applied.
-- ============================================================

CREATE TEMP TABLE seed_coa (
  code         VARCHAR(30)  PRIMARY KEY,
  parent_code  VARCHAR(30),
  type_code    VARCHAR(20)  NOT NULL,
  nature_code  VARCHAR(10)  NOT NULL,
  is_leaf_fl   BOOLEAN      NOT NULL,
  name_ar      VARCHAR(200) NOT NULL,
  name_en      VARCHAR(200) NOT NULL,
  old_code     VARCHAR(30)            -- legacy account reused by this row, when one exists
) ON COMMIT DROP;

INSERT INTO seed_coa (code, parent_code, type_code, nature_code, is_leaf_fl, name_ar, name_en, old_code) VALUES
-- ---------------------------------------------------------------- 1 ASSETS
('1',      NULL,   'ASSET','DEBIT', FALSE,'الأصول','Assets','0001'),
('11',     '1',    'ASSET','DEBIT', FALSE,'الأصول المتداولة','Current assets','00010002'),
('1101',   '11',   'ASSET','DEBIT', FALSE,'النقد وما في حكمه','Cash and cash equivalents','000100020001'),
('110101', '1101', 'ASSET','DEBIT', TRUE, 'الصندوق الرئيسي','Main cash box','0001000200010001'),
('110102', '1101', 'ASSET','DEBIT', TRUE, 'صناديق الفروع','Branch cash boxes','0001000200010002'),
('110103', '1101', 'ASSET','DEBIT', TRUE, 'نقد بالطريق وتحويلات بنكية','Cash in transit and bank transfers','0001000200010003'),
('110111', '1101', 'ASSET','DEBIT', TRUE, 'البنك التجاري','Commercial Bank','0001000200010007'),
('110112', '1101', 'ASSET','DEBIT', TRUE, 'مصرف الراجحي','Al Rajhi Bank','0001000200010005'),
('110113', '1101', 'ASSET','DEBIT', TRUE, 'بيت التمويل','Finance House bank','0001000200010006'),
('110119', '1101', 'ASSET','DEBIT', TRUE, 'بنوك أخرى','Other banks','0001000200010004'),
('1102',   '11',   'ASSET','DEBIT', FALSE,'ذمم التقسيط المدينة','Instalment receivables','000100020003'),
('110201', '1102', 'ASSET','DEBIT', TRUE, 'ذمم عملاء التقسيط - منتظمة','Instalment customers - performing','0001000200030001'),
('110202', '1102', 'ASSET','DEBIT', TRUE, 'ذمم عملاء التقسيط - متعثرة وتحت الإجراء القضائي','Instalment customers - defaulted / in litigation',NULL),
('110203', '1102', 'ASSET','CREDIT',TRUE, '(-) إيرادات تقسيط مؤجلة','Less: deferred instalment income','000200010099'),
('110204', '1102', 'ASSET','CREDIT',TRUE, '(-) مخصص الخسائر الائتمانية المتوقعة','Less: expected credit loss allowance','0002000100150015'),
('110205', '1102', 'ASSET','DEBIT', TRUE, 'شيكات أقساط مؤجلة الدفع','Post-dated instalment cheques',NULL),
('110206', '1102', 'ASSET','DEBIT', TRUE, 'شيكات تحت التحصيل','Cheques under collection','0001000200090001'),
('1103',   '11',   'ASSET','DEBIT', FALSE,'ذمم مدينة أخرى','Other receivables','000100020014'),
('110301', '1103', 'ASSET','DEBIT', TRUE, 'مصاريف قضائية مستردة من العملاء','Legal costs recoverable from customers',NULL),
('110302', '1103', 'ASSET','DEBIT', TRUE, 'مستحقات لدى المحكمة وإدارة التنفيذ','Amounts held by court and enforcement',NULL),
('110303', '1103', 'ASSET','DEBIT', TRUE, 'عهد المحاماة والمحكمة','Lawyer and court custody advances','0001000200060001'),
('110304', '1103', 'ASSET','DEBIT', TRUE, 'سلف العاملين','Employee advances','0001000200050001'),
('110305', '1103', 'ASSET','DEBIT', TRUE, 'عهد مؤقتة للموظفين','Temporary employee custody',NULL),
('110306', '1103', 'ASSET','DEBIT', TRUE, 'عهد مستديمة','Permanent custody (imprest)','0001000200060004'),
('110307', '1103', 'ASSET','DEBIT', TRUE, 'قروض ممنوحة للمستثمرين','Loans granted to investors','0001000200140001'),
('110308', '1103', 'ASSET','DEBIT', TRUE, 'أرصدة مدينة أخرى','Other debit balances',NULL),
('1104',   '11',   'ASSET','DEBIT', FALSE,'المخزون','Inventory','000100020002'),
('110401', '1104', 'ASSET','DEBIT', TRUE, 'بضاعة بالمخازن','Goods in stock','0001000200020001'),
('110402', '1104', 'ASSET','DEBIT', TRUE, 'بضاعة بالطريق','Goods in transit','0001000200020008'),
('1105',   '11',   'ASSET','DEBIT', FALSE,'مدفوعات مقدمة','Prepayments','000100020010'),
('110501', '1105', 'ASSET','DEBIT', TRUE, 'إيجارات مدفوعة مقدما','Prepaid rent','0001000200100001'),
('110502', '1105', 'ASSET','DEBIT', TRUE, 'مصروفات مدفوعة مقدما أخرى','Other prepaid expenses','0001000200100003'),
('110503', '1105', 'ASSET','DEBIT', TRUE, 'دفعات مقدمة للموردين','Advances to suppliers',NULL),
('12',     '1',    'ASSET','DEBIT', FALSE,'الأصول غير المتداولة','Non-current assets','00010001'),
('1201',   '12',   'ASSET','DEBIT', TRUE, 'ذمم تقسيط طويلة الأجل','Non-current instalment receivables',NULL),
('1202',   '12',   'ASSET','DEBIT', FALSE,'الممتلكات والمعدات','Property and equipment','000100010001'),
('120201', '1202', 'ASSET','DEBIT', TRUE, 'أراضي','Land','0001000100010001'),
('120202', '1202', 'ASSET','DEBIT', TRUE, 'مباني','Buildings','0001000100010002'),
('120203', '1202', 'ASSET','DEBIT', TRUE, 'وسائل نقل','Vehicles','0001000100010004'),
('120204', '1202', 'ASSET','DEBIT', TRUE, 'أثاث ومعدات مكتبية','Office furniture and equipment','0001000100010008'),
('120205', '1202', 'ASSET','DEBIT', TRUE, 'أجهزة حاسب آلي وطابعات','Computers and printers','0001000100010011'),
('1203',   '12',   'ASSET','CREDIT',FALSE,'(-) مجمع إهلاك الممتلكات والمعدات','Less: accumulated depreciation',NULL),
('120302', '1203', 'ASSET','CREDIT',TRUE, 'مجمع إهلاك مباني','Accumulated depreciation - buildings','0002000100150004'),
('120303', '1203', 'ASSET','CREDIT',TRUE, 'مجمع إهلاك وسائل نقل','Accumulated depreciation - vehicles','0002000100150006'),
('120304', '1203', 'ASSET','CREDIT',TRUE, 'مجمع إهلاك أثاث ومعدات مكتبية','Accumulated depreciation - furniture','0002000100150009'),
('120305', '1203', 'ASSET','CREDIT',TRUE, 'مجمع إهلاك أجهزة حاسب آلي','Accumulated depreciation - computers','0002000100150012'),
('1204',   '12',   'ASSET','DEBIT', FALSE,'أصول غير ملموسة','Intangible assets',NULL),
('120401', '1204', 'ASSET','DEBIT', TRUE, 'برامج حاسب آلي','Software','0001000100010010'),
('120402', '1204', 'ASSET','CREDIT',TRUE, '(-) مجمع إطفاء البرامج','Less: accumulated amortisation - software','0002000100150011'),
('1205',   '12',   'ASSET','DEBIT', TRUE, 'مشروعات تحت التنفيذ','Capital work in progress','000100010002'),
('1206',   '12',   'ASSET','DEBIT', TRUE, 'تأمينات وودائع لدى الغير','Deposits with others','000100020007'),
-- ---------------------------------------------------------------- 2 LIABILITIES
('2',      NULL,   'LIABILITY','CREDIT',FALSE,'الخصوم','Liabilities','0002'),
('21',     '2',    'LIABILITY','CREDIT',FALSE,'الخصوم المتداولة','Current liabilities','00020001'),
('2101',   '21',   'LIABILITY','CREDIT',FALSE,'الموردون','Suppliers','000200010005'),
('210101', '2101', 'LIABILITY','CREDIT',TRUE, 'موردون محليون','Local suppliers','0002000100050001'),
('210102', '2101', 'LIABILITY','CREDIT',TRUE, 'موردون خارجيون','Foreign suppliers','0002000100050002'),
('2102',   '21',   'LIABILITY','CREDIT',FALSE,'جاري المستثمرين','Investors current accounts',NULL),
('210201', '2102', 'LIABILITY','CREDIT',TRUE, 'جاري المستثمرين','Investors current accounts','0001000200130001'),
('2103',   '21',   'LIABILITY','CREDIT',FALSE,'أرصدة دائنة للعملاء','Customer credit balances',NULL),
('210301', '2103', 'LIABILITY','CREDIT',TRUE, 'مبالغ مستحقة الرد للعملاء','Customer refunds payable',NULL),
('210302', '2103', 'LIABILITY','CREDIT',TRUE, 'دفعات مقدمة من العملاء','Customer advances',NULL),
('2104',   '21',   'LIABILITY','CREDIT',FALSE,'مصروفات مستحقة','Accrued expenses','000200010003'),
('210401', '2104', 'LIABILITY','CREDIT',TRUE, 'أتعاب مراجعة مستحقة','Accrued audit fees','0002000100030001'),
('210402', '2104', 'LIABILITY','CREDIT',TRUE, 'مصروفات مستحقة أخرى','Other accrued expenses',NULL),
('2105',   '21',   'LIABILITY','CREDIT',FALSE,'مستحقات الموظفين','Employee payables','000200010006'),
('210501', '2105', 'LIABILITY','CREDIT',TRUE, 'رواتب مستحقة','Salaries payable','0002000100060001'),
('210502', '2105', 'LIABILITY','CREDIT',TRUE, 'إجازات مستحقة','Leave pay payable','0002000100060002'),
('210503', '2105', 'LIABILITY','CREDIT',TRUE, 'تذاكر سفر مستحقة','Air tickets payable','0002000100060003'),
('2106',   '21',   'LIABILITY','CREDIT',FALSE,'أوراق الدفع','Notes payable','000200010010'),
('210601', '2106', 'LIABILITY','CREDIT',TRUE, 'شيكات تحت الدفع','Cheques payable','0002000100100001'),
('2107',   '21',   'LIABILITY','CREDIT',TRUE, 'الزكاة وضرائب الشركات المستحقة','Zakat and corporate levies payable','0002000100150017'),
('2108',   '21',   'LIABILITY','CREDIT',TRUE, 'الجزء المتداول من القروض','Current portion of loans',NULL),
('2109',   '21',   'LIABILITY','CREDIT',TRUE, 'دائنون متنوعون','Sundry creditors','000200010013'),
('22',     '2',    'LIABILITY','CREDIT',FALSE,'الخصوم غير المتداولة','Non-current liabilities','00020002'),
('2201',   '22',   'LIABILITY','CREDIT',TRUE, 'قروض طويلة الأجل','Long-term loans','000200010014'),
('2202',   '22',   'LIABILITY','CREDIT',TRUE, 'مخصص مكافأة نهاية الخدمة','End-of-service benefits provision','0002000100150002'),
-- ---------------------------------------------------------------- 3 EQUITY
('3',      NULL,   'EQUITY','CREDIT',FALSE,'حقوق الملكية','Equity','0005'),
('3101',   '3',    'EQUITY','CREDIT',FALSE,'رأس المال','Share capital','00050001'),
('310101', '3101', 'EQUITY','CREDIT',TRUE, 'رأس المال المدفوع','Paid-up capital','0002000100040002'),
('3201',   '3',    'EQUITY','CREDIT',TRUE, 'الاحتياطي القانوني','Statutory reserve','0002000100010004'),
('3202',   '3',    'EQUITY','CREDIT',TRUE, 'احتياطيات أخرى','Other reserves','0002000100010003'),
('3301',   '3',    'EQUITY','CREDIT',FALSE,'الأرباح المبقاة','Retained earnings',NULL),
('330101', '3301', 'EQUITY','CREDIT',TRUE, 'الأرباح المبقاة المرحلة','Retained earnings brought forward','3200'),
('3401',   '3',    'EQUITY','CREDIT',FALSE,'جاري الشركاء','Partners current accounts','000200010001'),
('340101', '3401', 'EQUITY','CREDIT',TRUE, 'جاري الشريك - علوش','Partner current account - Alloush','0002000100010001'),
('340102', '3401', 'EQUITY','CREDIT',TRUE, 'جاري الشريك 2','Partner current account - 2','0002000100010002'),
-- ---------------------------------------------------------------- 4 REVENUE
('4',      NULL,   'REVENUE','CREDIT',FALSE,'الإيرادات','Revenue','0004'),
('41',     '4',    'REVENUE','CREDIT',FALSE,'إيرادات المبيعات','Sales revenue',NULL),
('4101',   '41',   'REVENUE','CREDIT',TRUE, 'مبيعات التقسيط','Instalment sales','00040005'),
('4102',   '41',   'REVENUE','CREDIT',TRUE, 'مبيعات نقدية','Cash sales',NULL),
('42',     '4',    'REVENUE','CREDIT',FALSE,'إيرادات التمويل','Finance income',NULL),
('4201',   '42',   'REVENUE','CREDIT',TRUE, 'أرباح التقسيط المحققة','Earned instalment income',NULL),
('43',     '4',    'REVENUE','CREDIT',FALSE,'إيرادات إدارة المستثمرين','Investor management income',NULL),
('4301',   '43',   'REVENUE','CREDIT',TRUE, 'عمولات وأتعاب إدارة المستثمرين','Investor management fees',NULL),
('44',     '4',    'REVENUE','CREDIT',FALSE,'إيرادات رسوم وخدمات','Fee and service income',NULL),
('4401',   '44',   'REVENUE','CREDIT',TRUE, 'إيرادات عقود','Contract income','00040003'),
('4402',   '44',   'REVENUE','CREDIT',TRUE, 'إيرادات تسديد عملاء','Customer settlement income','00040001'),
('4403',   '44',   'REVENUE','CREDIT',TRUE, 'إيرادات استرداد أتعاب قضائية','Recovered legal fees','0003000300040020'),
('45',     '4',    'REVENUE','CREDIT',FALSE,'إيرادات أخرى','Other income',NULL),
('4501',   '45',   'REVENUE','CREDIT',TRUE, 'إيرادات أخرى','Other income','00040006'),
('4502',   '45',   'REVENUE','CREDIT',TRUE, 'خصم مكتسب','Discount received',NULL),
('4503',   '45',   'REVENUE','CREDIT',TRUE, 'أرباح بيع أصول ثابتة','Gain on disposal of assets',NULL),
('49',     '4',    'REVENUE','DEBIT', FALSE,'(-) خصومات ومخالصات ممنوحة','Less: discounts and settlements granted',NULL),
('4901',   '49',   'REVENUE','DEBIT', TRUE, 'خصومات مخالصات العقود','Contract settlement discounts','0003000200030002'),
-- ---------------------------------------------------------------- 5 COST OF REVENUE
('5',      NULL,   'EXPENSE','DEBIT', FALSE,'تكلفة الإيرادات','Cost of revenue',NULL),
('51',     '5',    'EXPENSE','DEBIT', FALSE,'تكلفة المبيعات','Cost of sales',NULL),
('5101',   '51',   'EXPENSE','DEBIT', TRUE, 'تكلفة البضاعة المباعة','Cost of goods sold',NULL),
('5102',   '51',   'EXPENSE','DEBIT', TRUE, 'مشتريات بضاعة','Purchases of goods','00030003000400330002'),
('5103',   '51',   'EXPENSE','DEBIT', TRUE, 'مصاريف شراء ونقل بضاعة','Purchase and freight costs',NULL),
-- ---------------------------------------------------------------- 6 OPERATING EXPENSES (department = dimension)
('6',      NULL,   'EXPENSE','DEBIT', FALSE,'المصروفات التشغيلية','Operating expenses','0003'),
('61',     '6',    'EXPENSE','DEBIT', FALSE,'الرواتب والأجور وما في حكمها','Salaries, wages and benefits',NULL),
('6101',   '61',   'EXPENSE','DEBIT', TRUE, 'رواتب أساسية','Basic salaries','0003000100010001'),
('6102',   '61',   'EXPENSE','DEBIT', TRUE, 'بدلات','Allowances',NULL),
('6103',   '61',   'EXPENSE','DEBIT', TRUE, 'مكافآت وحوافز','Bonuses and incentives',NULL),
('6104',   '61',   'EXPENSE','DEBIT', TRUE, 'تأمينات اجتماعية','Social insurance',NULL),
('6105',   '61',   'EXPENSE','DEBIT', TRUE, 'مكافأة نهاية الخدمة','End-of-service benefits',NULL),
('6106',   '61',   'EXPENSE','DEBIT', TRUE, 'إقامات وتأشيرات','Residency and visas',NULL),
('6107',   '61',   'EXPENSE','DEBIT', TRUE, 'علاج وتأمين صحي','Medical and health insurance',NULL),
('62',     '6',    'EXPENSE','DEBIT', FALSE,'الإيجارات والمرافق','Rent and utilities',NULL),
('6201',   '62',   'EXPENSE','DEBIT', TRUE, 'إيجارات','Rent','0003000100040003'),
('6202',   '62',   'EXPENSE','DEBIT', TRUE, 'كهرباء ومياه','Electricity and water',NULL),
('6203',   '62',   'EXPENSE','DEBIT', TRUE, 'هاتف واتصالات وبريد','Telephone, communications and post',NULL),
('63',     '6',    'EXPENSE','DEBIT', FALSE,'الصيانة والمحروقات','Maintenance and fuel',NULL),
('6301',   '63',   'EXPENSE','DEBIT', TRUE, 'صيانة سيارات ومحروقات','Vehicle maintenance and fuel',NULL),
('6302',   '63',   'EXPENSE','DEBIT', TRUE, 'صيانة مباني وأثاث','Building and furniture maintenance',NULL),
('6303',   '63',   'EXPENSE','DEBIT', TRUE, 'صيانة برامج وأجهزة','IT maintenance',NULL),
('6304',   '63',   'EXPENSE','DEBIT', TRUE, 'تأمين سيارات','Vehicle insurance',NULL),
('64',     '6',    'EXPENSE','DEBIT', FALSE,'الإهلاك والإطفاء','Depreciation and amortisation',NULL),
('6401',   '64',   'EXPENSE','DEBIT', TRUE, 'مصروف الإهلاك','Depreciation expense',NULL),
('6402',   '64',   'EXPENSE','DEBIT', TRUE, 'مصروف الإطفاء','Amortisation expense',NULL),
('65',     '6',    'EXPENSE','DEBIT', FALSE,'مصاريف التحصيل والقضايا','Collection and legal expenses',NULL),
('6501',   '65',   'EXPENSE','DEBIT', TRUE, 'أتعاب محاماة','Lawyer fees',NULL),
('6502',   '65',   'EXPENSE','DEBIT', TRUE, 'رسوم تنفيذ وقضايا','Enforcement and court fees',NULL),
('66',     '6',    'EXPENSE','DEBIT', FALSE,'الخسائر الائتمانية','Credit losses',NULL),
('6601',   '66',   'EXPENSE','DEBIT', TRUE, 'مصروف الخسائر الائتمانية المتوقعة','Expected credit loss expense',NULL),
('6602',   '66',   'EXPENSE','DEBIT', TRUE, 'ديون معدومة ومخالصات عملاء متعثرين','Bad debts and defaulted-customer settlements',NULL),
('67',     '6',    'EXPENSE','DEBIT', FALSE,'مصاريف البيع والتسويق','Selling and marketing expenses',NULL),
('6701',   '67',   'EXPENSE','DEBIT', TRUE, 'عمولات بيع','Sales commissions',NULL),
('6702',   '67',   'EXPENSE','DEBIT', TRUE, 'دعاية وإعلان','Advertising',NULL),
('68',     '6',    'EXPENSE','DEBIT', FALSE,'مصاريف عمومية وإدارية','General and administrative expenses',NULL),
('6801',   '68',   'EXPENSE','DEBIT', TRUE, 'مصروفات عمومية متنوعة','Sundry general expenses','0003000300040031'),
('6802',   '68',   'EXPENSE','DEBIT', TRUE, 'قرطاسية ومطبوعات','Stationery and printing',NULL),
('6803',   '68',   'EXPENSE','DEBIT', TRUE, 'ضيافة ونظافة','Hospitality and cleaning',NULL),
('6804',   '68',   'EXPENSE','DEBIT', TRUE, 'رسوم حكومية واشتراكات','Government fees and subscriptions',NULL),
('6805',   '68',   'EXPENSE','DEBIT', TRUE, 'عمولات بنكية','Bank charges',NULL),
('6806',   '68',   'EXPENSE','DEBIT', TRUE, 'أتعاب مهنية واستشارات','Professional and consulting fees',NULL),
('6807',   '68',   'EXPENSE','DEBIT', TRUE, 'خسائر بيع أصول ثابتة','Loss on disposal of assets',NULL),
('6808',   '68',   'EXPENSE','DEBIT', TRUE, 'تبرعات','Donations',NULL),
('6899',   '68',   'EXPENSE','DEBIT', TRUE, 'مصروفات أخرى','Other expenses','0003000100040029'),
('69',     '6',    'EXPENSE','DEBIT', FALSE,'الزكاة والضرائب','Zakat and taxes',NULL),
('6901',   '69',   'EXPENSE','DEBIT', TRUE, 'الزكاة وضرائب الشركات','Zakat and corporate levies',NULL);

-- ----------------------------------------------------------------------------
-- 2. CONSTANT rule lines follow their account to the new code (before the recode below)
-- ----------------------------------------------------------------------------
UPDATE fin_rule_line l
   SET account_derivation_value = s.code
  FROM seed_coa s
 WHERE l.account_derivation_type_code = 'CONSTANT'
   AND s.old_code IS NOT NULL
   AND l.account_derivation_value = s.old_code;

-- ----------------------------------------------------------------------------
-- 3. accounts: rewrite the reused ones, insert the rest, then link parents
-- ----------------------------------------------------------------------------
UPDATE fin_account a
   SET code = s.code, name_ar = s.name_ar, name_en = s.name_en,
       account_type_code = s.type_code, nature_code = s.nature_code,
       is_leaf_fl = s.is_leaf_fl, is_active_fl = TRUE,
       updated_by = 'SYSTEM', updated_at = now()
  FROM seed_coa s
 WHERE s.old_code IS NOT NULL AND a.code = s.old_code;

INSERT INTO fin_account (account_pk, code, name_ar, name_en, account_type_code, nature_code,
                         parent_account_id, is_leaf_fl, is_active_fl, created_by, created_at)
SELECT nextval('seq_fin_account'), s.code, s.name_ar, s.name_en, s.type_code, s.nature_code,
       NULL, s.is_leaf_fl, TRUE, 'SYSTEM', now()
  FROM seed_coa s
 WHERE NOT EXISTS (SELECT 1 FROM fin_account a WHERE a.code = s.code);

UPDATE fin_account a
   SET parent_account_id = p.account_pk
  FROM seed_coa s LEFT JOIN fin_account p ON p.code = s.parent_code
 WHERE a.code = s.code
   AND a.parent_account_id IS DISTINCT FROM p.account_pk;

-- ----------------------------------------------------------------------------
-- 4. rule corrections called for by the review (§2-C)
-- ----------------------------------------------------------------------------
-- COMPLAINT_SETTLEMENT: the discount granted to a customer in a COMPLAINT (a defaulted customer) is a
-- credit loss, not a sales discount. CONTRACT_SETTLEMENT keeps 4901 (contra-revenue).
UPDATE fin_rule_line l
   SET account_derivation_value = '6602'
  FROM fin_event_type_rule r
 WHERE r.event_type_rule_pk = l.event_type_rule_id
   AND r.event_type_code IN ('COMPLAINT_SETTLEMENT', 'COMPLAINT_SETTLEMENT_REVERSED')
   AND l.account_derivation_type_code = 'CONSTANT'
   AND l.account_derivation_value = '4901';
-- LAWYER_FEE_PAYMENT (COMPLAINT_DT_FL = 44, the customer pays the lawyer-fee instalments) used to
-- CREDIT an expense account; it now credits 4403 'recovered legal fees' — handled by the reuse of the
-- legacy account '0003000300040020' as 4403 in seed_coa, nothing more to do here.

-- ----------------------------------------------------------------------------
-- 5. retire the legacy template: delete what nothing references, archive what something does
-- ----------------------------------------------------------------------------
CREATE TEMP TABLE coa_referenced ON COMMIT DROP AS
SELECT account_id AS account_pk FROM fin_journal_line
UNION SELECT account_id FROM fin_account_mapping
UNION SELECT account_id FROM fin_recurring_template_line
UNION SELECT source_account_id FROM fin_allocation_rule
UNION SELECT target_account_id FROM fin_allocation_target
UNION SELECT a.account_pk FROM fin_rule_line l JOIN fin_account a ON a.code = l.account_derivation_value
       WHERE l.account_derivation_type_code = 'CONSTANT'
UNION SELECT account_pk FROM fin_account WHERE is_retained_earnings_fl;

DO $$
DECLARE n INT; total INT := 0;
BEGIN
  LOOP
    DELETE FROM fin_account a
     WHERE NOT EXISTS (SELECT 1 FROM seed_coa s WHERE s.code = a.code)
       AND NOT EXISTS (SELECT 1 FROM coa_referenced r WHERE r.account_pk = a.account_pk)
       AND NOT EXISTS (SELECT 1 FROM fin_account c WHERE c.parent_account_id = a.account_pk);
    GET DIAGNOSTICS n = ROW_COUNT;
    total := total + n;
    EXIT WHEN n = 0;
  END LOOP;
  RAISE NOTICE 'V40: % legacy template accounts deleted', total;
END $$;

-- Anything left outside the target chart is referenced (only test data in a dev DB): keep it for its
-- history, deactivated, under one inactive archive node per class. It cannot be posted to again.
DO $$
DECLARE r RECORD; v_arch BIGINT; v_root VARCHAR(1); n INT := 0;
BEGIN
  FOR r IN SELECT a.account_pk, a.account_type_code FROM fin_account a
            WHERE NOT EXISTS (SELECT 1 FROM seed_coa s WHERE s.code = a.code)
              AND a.code NOT IN ('1999', '2999', '3999', '4999', '6999') LOOP
    v_root := CASE r.account_type_code WHEN 'ASSET' THEN '1' WHEN 'LIABILITY' THEN '2' WHEN 'EQUITY' THEN '3'
                                       WHEN 'REVENUE' THEN '4' ELSE '6' END;
    SELECT account_pk INTO v_arch FROM fin_account WHERE code = v_root || '999';
    IF v_arch IS NULL THEN
      v_arch := nextval('seq_fin_account');
      INSERT INTO fin_account (account_pk, code, name_ar, name_en, account_type_code, nature_code,
                               parent_account_id, is_leaf_fl, is_active_fl, created_by, created_at)
      SELECT v_arch, v_root || '999', 'حسابات مؤرشفة (غير مستخدمة)', 'Archived accounts (not in use)',
             p.account_type_code, p.nature_code, p.account_pk, FALSE, FALSE, 'SYSTEM', now()
        FROM fin_account p WHERE p.code = v_root;
    END IF;
    UPDATE fin_account SET parent_account_id = v_arch, is_active_fl = FALSE,
                           updated_by = 'SYSTEM', updated_at = now()
     WHERE account_pk = r.account_pk;
    n := n + 1;
  END LOOP;
  -- an archived non-leaf keeps its archived children; nothing else may hang below the archive
  RAISE NOTICE 'V40: % referenced legacy accounts archived (inactive)', n;
END $$;

-- ----------------------------------------------------------------------------
-- 6. verification — any failure rolls the whole migration back
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_list TEXT; v_count INT;
BEGIN
  SELECT count(*) INTO v_count FROM seed_coa s WHERE NOT EXISTS (SELECT 1 FROM fin_account a WHERE a.code = s.code);
  IF v_count > 0 THEN RAISE EXCEPTION 'V40 6.1: % target accounts missing', v_count; END IF;

  SELECT string_agg(a.code, ', ') INTO v_list FROM fin_account a JOIN seed_coa s ON s.code = a.code
   WHERE a.parent_account_id IS DISTINCT FROM (SELECT p.account_pk FROM fin_account p WHERE p.code = s.parent_code);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'V40 6.2: wrong parent: %', v_list; END IF;

  SELECT string_agg(a.code, ', ') INTO v_list FROM fin_account a
   WHERE a.is_leaf_fl AND EXISTS (SELECT 1 FROM fin_account c WHERE c.parent_account_id = a.account_pk);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'V40 6.3: leaf accounts with children: %', v_list; END IF;

  SELECT string_agg(c.code, ', ') INTO v_list FROM fin_account c JOIN fin_account p ON p.account_pk = c.parent_account_id
   WHERE c.account_type_code <> p.account_type_code;
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'V40 6.4: child/parent account type mismatch: %', v_list; END IF;

  SELECT count(*) INTO v_count FROM fin_account WHERE is_retained_earnings_fl AND code = '330101' AND is_leaf_fl AND is_active_fl;
  IF v_count <> 1 THEN RAISE EXCEPTION 'V40 6.5: retained earnings is not the active leaf 330101'; END IF;

  -- every ACTIVE rule's CONSTANT line names an active leaf
  SELECT string_agg(r.event_type_code || ':' || l.line_no || '=' || COALESCE(l.account_derivation_value, 'NULL'), ', ') INTO v_list
    FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id
   WHERE r.is_active_fl AND l.account_derivation_type_code = 'CONSTANT'
     AND NOT EXISTS (SELECT 1 FROM fin_account a WHERE a.code = l.account_derivation_value AND a.is_leaf_fl AND a.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'V40 6.6: active rule lines on a missing / non-leaf / inactive account: %', v_list; END IF;

  -- every ACTIVE mapping points at an active leaf
  SELECT string_agg(m.event_type_code || '/' || m.business_value, ', ') INTO v_list
    FROM fin_account_mapping m JOIN fin_account a ON a.account_pk = m.account_id
   WHERE m.is_active_fl AND NOT (a.is_leaf_fl AND a.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'V40 6.7: active mappings on a non-leaf / inactive account: %', v_list; END IF;

  -- the root of every active account is one of the six classes
  SELECT count(*) INTO v_count FROM fin_account WHERE parent_account_id IS NULL AND code NOT IN ('1','2','3','4','5','6');
  IF v_count > 0 THEN RAISE EXCEPTION 'V40 6.8: % root accounts outside the six classes', v_count; END IF;

  RAISE NOTICE 'V40: chart restructured — % accounts (% active leaves)',
    (SELECT count(*) FROM fin_account), (SELECT count(*) FROM fin_account WHERE is_leaf_fl AND is_active_fl);
END $$;
