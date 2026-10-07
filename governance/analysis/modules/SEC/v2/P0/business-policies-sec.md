## BUSINESS POLICIES — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module   : SEC     Source of truth : new project/security-module-plan-en.md + مانيفست التغيير v2 + قرارات الحوار
Read by  : P0.5 (every user story cites the policies it serves)
Delta    : v2 — سياسات مُضافة فقط؛ سياسات v1 كلها باقية دون تعديل وتسري على حساب الخدمة كما تسري على أي أساس أمني
══════════════════════════════════════════════════════════════════

CLIENT-SPECIFIC POLICIES   (only from user text or confirmed dialogue answers)

POL-SEC-012 — تكافؤ حساب الخدمة / Service account principal parity
  Statement (ar) : يجب على النظام معاملة حساب الخدمة كأساس أمني يحمل الأدوار والمنح تمامًا كما يحملها المستخدم البشري، خاضعًا لكل سياسات التفويض القائمة في هذه الوحدة.
  Statement (en) : The system shall treat a service account as a principal that holds roles and grants exactly as a human user does, subject to every existing authorization policy of this module.
  Pattern   : ubiquitous
  Trigger   : Any authorization check; role assignment
  Rationale : ثوابت v1 موروثة لا يُعاد فتحها — حساب الخدمة أساس أمني كغيره
  Source    : مانيفست v2 §1 ("hold permissions like any other principal")؛ معيار القبول 2؛ قرار الحوار في module-registry-sec.md #2
  Status    : CONFIRMED

POL-SEC-013 — لا صلاحية ضمنية عند الإنشاء / No implicit permission at creation
  Statement (ar) : عند إنشاء حساب خدمة، يجب على النظام ألا يُسند إليه أي دور أو منح إلى أن يُسندها مسؤول صراحةً.
  Statement (en) : When a service account is created, the system shall assign it no role and no grant until an administrator assigns them explicitly.
  Pattern   : event
  Trigger   : Create (service account)
  Rationale : المستهلك يحتاج منح إجراء واحدًا فقط (إنشاء قيد يومية في FIN) ولا شيء غيره
  Source    : مانيفست v2 §1 ("exactly one … and nothing else")؛ قرار الحوار #4 أدناه
  Status    : CONFIRMED

POL-SEC-014 — مصادقة آلية دون جلسة بشرية / Unattended authentication without a human session
  Statement (ar) : عندما يقدّم حساب خدمة بيانات اعتماد نشطة، يجب على النظام مصادقته دون أي دخول تفاعلي ودون الاعتماد على جلسة دخول أي مستخدم بشري.
  Statement (en) : When a service account presents an active credential, the system shall authenticate it without any interactive login and without relying on any human user's login session.
  Pattern   : event
  Trigger   : Machine authentication request
  Rationale : تشغيل البرنامج الخدمي بهوية بشرية يربط التكامل بجلسة شخص
  Source    : مانيفست v2 §1 ("does not depend on a human login session")؛ قرار الحوار في module-registry-sec.md #1
  Status    : CONFIRMED

POL-SEC-015 — الاستمرار دون تدخّل بشري / Survives unattended
  Statement (ar) : أثناء بقاء حساب الخدمة وبيانات اعتماده نشطين، يجب على النظام إبقاء قدرته على المصادقة قائمة دون إعادة مصادقة من أي شخص، بصرف النظر عن انتهاء الجلسات أو تنظيفها الدوري.
  Statement (en) : While a service account and its credential remain active, the system shall keep the account able to authenticate without any person re-authenticating it, regardless of the expiry or routine cleanup of login sessions.
  Pattern   : state
  Trigger   : Continuous operation; session expiry; session cleanup
  Rationale : التنظيف الدوري للجلسات يُسقط التكامل بصمت
  Source    : مانيفست v2 §1 ("survives unattended for as long as the service runs")؛ قرار الحوار في module-registry-sec.md #3
  Status    : CONFIRMED

POL-SEC-016 — منع الاستخدام التفاعلي / No interactive use
  Statement (ar) : إذا طُلب دخول تفاعلي أو إعادة تعيين كلمة مرور لحساب خدمة، فيجب على النظام رفض الطلب.
  Statement (en) : If an interactive login or a password reset is requested for a service account, then the system shall reject the request.
  Pattern   : unwanted
  Trigger   : Login; password-reset request
  Rationale : حساب الخدمة مُستدعٍ آلي فقط؛ كل مسار تفاعلي له سطح هجوم بلا حاجة
  Source    : قرار الحوار #2 أدناه (مُعدَّل في الجولة 3 — التسجيل الذاتي انتقل إلى POL-SEC-022)
  Status    : CONFIRMED

POL-SEC-017 — الإلغاء نافذ من الطلب التالي، بلا نافذة ثقة برمز سابق / Revocation effective on the next request, with no trust window for a previously issued token
  Statement (ar) : عندما يُلغي مسؤول بيانات اعتماد حساب خدمة أو يُعطّل الحساب، يجب على النظام رفض كل طلب لاحق يُصادَق ببيانات الاعتماد الملغاة، أو بأيٍّ من بيانات اعتماد الحساب المعطَّل، أو برمز وصول صادر عن أيٍّ منها.
  Statement (en) : When an administrator revokes a service account's credential or deactivates the service account, the system shall reject every subsequent request authenticated by the revoked credential, by any credential of the deactivated account, or by any access token issued from such a credential.
  Pattern   : event
  Trigger   : Revoke credential; deactivate service account
  Rationale : حساب الخدمة يجب أن يكون قابلًا للإلغاء من الإدارة القائمة؛ ولأن المصادقة تتم عبر بادلة سرّ برمز وصول قصير العمر (قرار #1)، فإن التحقق يجب أن يشمل الرمز في كل طلب لا السرّ فقط — وإلا بقي رمز صادر قبل الإلغاء صالحًا حتى انتهاء صلاحيته، فيتكرر عطل "التنظيف الصامت" الذي فتح هذا التغيير من زاوية أخرى
  Source    : مانيفست v2 §1 ("visible and revocable")؛ قرار الحوار في module-registry-sec.md #3 (مُعدَّل في الجولة 2؛ صياغة التعطيل مُوضَّحة في الجولة 3 — قرار الحوار #5 أدناه)
  Status    : CONFIRMED

POL-SEC-018 — الظهور في الإدارة القائمة / Visible in existing administration
  Statement (ar) : يجب على النظام عرض حسابات الخدمة ضمن شاشات إدارة الأمان القائمة، مميَّزةً عن المستخدمين البشريين، وإدارتها بنفس إدارة الأدوار والمنح.
  Statement (en) : The system shall present service accounts in the existing security administration screens, distinguishable from human users and administered through the same role and grant management.
  Pattern   : ubiquitous
  Trigger   : Administration views
  Rationale : لا إدارة موازية لحسابات الخدمة
  Source    : مانيفست v2 §1 ("visible … from SEC's existing administration screens")؛ قرار الحوار في module-registry-sec.md #2
  Status    : CONFIRMED

POL-SEC-019 — التدقيق كأي أساس أمني / Audited like any principal
  Statement (ar) : يجب على النظام تسجيل كل مصادقة ناجحة أو فاشلة لحساب الخدمة، وكل إصدار أو إلغاء لبيانات اعتماده، وكل تغيير في أدواره أو منحه، في سجل التدقيق منسوبًا إلى حساب الخدمة ذاته.
  Statement (en) : The system shall record every successful or failed authentication of a service account, every issuance or revocation of its credential, and every change to its roles or grants in the audit log, attributed to that service account.
  Pattern   : ubiquitous
  Trigger   : Machine authentication; credential issuance/revocation; role or grant change
  Rationale : نشاط حساب الخدمة يظهر في سجل التدقيق كأي أساس أمني
  Source    : مانيفست v2 §1 ("its activity shows up in the audit log like any other principal's")؛ قرار الحوار #3 أدناه
  Status    : CONFIRMED

POL-SEC-020 — كشف السرّ مرة واحدة / Secret revealed once
  Statement (ar) : يجب على النظام ألا يكشف سرّ بيانات اعتماد حساب الخدمة إلا مرة واحدة عند إصدارها، وألا يكشفه بعد ذلك مطلقًا.
  Statement (en) : The system shall reveal a service-account credential's secret only once, at issuance, and never afterwards.
  Pattern   : ubiquitous
  Trigger   : Credential issuance; any later administration response
  Rationale : امتداد لمبدأ v1 بعدم إرسال تجزئة كلمة المرور للعميل إلى أسرار الخدمة
  Source    : قرار الحوار #2 أدناه
  Status    : CONFIRMED

POL-SEC-021 — تدوير دون انقطاع / Rotation without downtime
  Statement (ar) : أثناء حمل حساب الخدمة أكثر من بيانات اعتماد نشطة، يجب على النظام قبول كلٍّ منها إلى أن تُلغى منفردة.
  Statement (en) : While a service account holds more than one active credential, the system shall accept each of them until that credential is individually revoked.
  Pattern   : state
  Trigger   : Credential rotation
  Rationale : تدوير السرّ دون إيقاف البرنامج الخدمي
  Source    : قرار الحوار #1 أدناه (النمط صُحِّح في الجولة 3 — قرار الحوار #5)
  Status    : CONFIRMED

POL-SEC-022 — الإنشاء عبر الإدارة فقط / Created only through administration
  Statement (ar) : يجب على النظام ألا ينشئ حساب خدمة إلا عبر إدارة الأمان، وألا ينشئه عبر التسجيل الذاتي مطلقًا.
  Statement (en) : The system shall create a service account only through the security administration and never through self sign-up.
  Pattern   : ubiquitous
  Trigger   : Create (service account); sign-up
  Rationale : حساب الخدمة لا "يسجّل نفسه"؛ التسجيل الذاتي في v1 يُنتج مستخدمًا بشريًا معلّقًا فقط، ووجود مسار ذاتي لإنشاء أساس آلي يتجاوز المسؤول
  Source    : قرار الحوار #2 أدناه (فُصل عن POL-SEC-016 في الجولة 3 — قرار الحوار #5)
  Status    : CONFIRMED

POL-SEC-023 — تخزين السرّ بصيغة غير قابلة للعكس / Secret retained only in non-reversible form
  Statement (ar) : يجب على النظام ألا يحتفظ بسرّ بيانات اعتماد حساب الخدمة إلا بصيغة غير قابلة للعكس.
  Statement (en) : The system shall retain a service-account credential's secret only in a non-reversible form.
  Pattern   : ubiquitous
  Trigger   : Credential issuance
  Rationale : سرّ الخدمة يحمل صلاحية الترحيل في FIN دون تدخّل بشري؛ تسريب مخزن البيانات يجب ألا يكشفه — نظير التخزين الآمن لكلمة المرور في v1
  Source    : قرار الحوار #2 أدناه (القرار نصّ على التخزين غير القابل للعكس ولم تحمله سياسة حتى الجولة 3 — قرار الحوار #5)؛ OWASP Secrets Management Cheat Sheet
  Status    : CONFIRMED

POL-SEC-024 — التكامل الاختياري ليس تبعية صلبة / An optional integration is never a hard dependency
  Statement (ar) : إذا كان تكامل منصّي مُعلَن اختياريًا غير متاح أو غير مُفعَّل، فيجب على النظام إتمام العملية التي يخدمها دون إخفاق، وتسجيل تعذّر التكامل.
  Statement (en) : If an integration the platform declares optional is unavailable or not enabled, then the system shall complete the operation it serves without failing, and record that the integration could not be reached.
  Pattern   : unwanted
  Trigger   : An optional platform integration is unavailable
  Rationale : v1 حملت هذا الشرط في نصّ القصة ("so that I am not blocked if this integration is skipped") دون سياسة تحمله، فبقيت US-SEC-012 بلا أثر إلى سياسة — وهو ما يرفضه C4.4. السياسة تنصّ على القاعدة التي كانت الوحدة تعمل بها فعلًا، ولا تغيّر سلوكًا.
  Source    : US-SEC-012؛ security-module-plan-en.md §8؛ new project/integration-notifications-fileservice.md §1؛ فجوة كشفها فحص C4.4 عند دلتا CS-SEC-001
  Status    : CONFIRMED

CUSTOM LOOKUP VALUES   (values the user named that the standard lists lack)
| Lookup key | Added values | Source |
|---|---|---|
None — standard values apply; القيم الأولية المُضافة تلقائيًا (PRINCIPAL_TYPE، امتداد AUDIT_EVENT_TYPE) مسجّلة في module-registry-sec.md → AUTO-DECISIONS لأنها ليست قيمًا ذكرها المستخدم.

SCOPE EXCEPTIONS   (explicit exclusions or non-standard scope)
(ADDED rows — صفوف v1 باقية)
| Excluded / Deferred | Statement | Activation trigger | Source |
|---|---|---|---|
| انتهاء إلزامي لبيانات اعتماد الخدمة / Forced service-credential expiry | لا تنتهي بيانات الاعتماد تلقائيًا؛ التدوير والإلغاء بيد المسؤول | طلب صريح من سياسة الأمان | قرار الحوار #1 أدناه |
| هوية أحمال العمل الموحَّدة وشهادات mTLS / Workload identity federation, mTLS | غير مبنية في هذا الإصدار | مُستدعون آليون خارجيون أو مستضافون سحابيًا | module-registry-sec.md قرار الحوار #1 |
| تسجيل كل استدعاء أعمال لحساب الخدمة داخل سجل SEC / SEC-side logging of every business call | الأفعال التجارية تُنسب عبر حقول التدقيق القياسية في الوحدة المالكة، لا في سجل SEC | حاجة تدقيق على مستوى المنصة — بقرار مستقل | قرار الحوار #3 أدناه |
| طابور قاعدة البيانات وخدمة المستهلك ونظام Oracle / Queue, consumer service, Oracle internals | خارج نطاق هذا التغيير | — | مانيفست v2 — Out of scope |

RESOLVED DECISIONS (dialogue, this module)
| # | Question | Recommended answer | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | عمر بيانات الاعتماد وتدويرها / Credential lifetime and rotation | لا انتهاء إلزامي؛ تدوير يُطلقه المسؤول بإصدار بيانات ثانية ثم إلغاء الأولى (POL-SEC-021) | Recommended by dialogue round 1; accepted unchanged in round 2 — converged | مانيفست v2 §1؛ RFC 6749 §4.4؛ ممارسة Google Cloud service-account keys ومقابلها Microsoft Entra client secrets |
| 2 | الاستخدام التفاعلي وكشف السرّ / Interactive use and secret disclosure | منع الدخول التفاعلي وإعادة التعيين (POL-SEC-016)؛ الإنشاء عبر الإدارة فقط لا بالتسجيل الذاتي (POL-SEC-022)؛ كشف السرّ مرة واحدة (POL-SEC-020) وتخزينه بصيغة غير قابلة للعكس (POL-SEC-023) | Recommended by dialogue round 1; accepted in round 2; policy split refined in round 3 — converged | security-module-plan-en.md §3؛ OWASP Secrets Management Cheat Sheet؛ ممارسة GitHub وStripe |
| 3 | نطاق "النشاط" في سجل التدقيق / Audit scope of service-account activity | سجل SEC يحمل المصادقات ودورة حياة بيانات الاعتماد وتغييرات المنح (POL-SEC-019)؛ الأفعال التجارية تُنسب عبر حقول التدقيق القياسية في الوحدة المالكة | Recommended by dialogue round 1; accepted unchanged in round 2 — converged | security-module-plan-en.md §5.2؛ [KB:erp-domain-standards §6]؛ module-registry-sec.md v1 AUTO-DECISIONS |
| 4 | "صلاحية واحدة بالضبط" ضمن النموذج الهرمي / "Exactly one permission" in the 3-level model | منح إجراء واحد (إنشاء قيد يومية)؛ منح وحدة FIN وشاشة القيد مسار بنيوي تفرضه بوابة الوحدة لا صلاحية إضافية؛ لا منح VIEW؛ لا منح ضمنية عند الإنشاء (POL-SEC-013) | Recommended by dialogue round 1; accepted unchanged in round 2 — converged | مانيفست v2 §1؛ security-module-plan-en.md §4.2؛ [KB:erp-domain-standards §4]؛ domain-profile §5 G1 |
| 5 | تنقيح صياغة السياسات في الجولة 3 / Round-3 policy wording refinements | (أ) POL-SEC-016 يخص طلبات الدخول وإعادة التعيين "لحساب خدمة" — حساب قائم لا "يسجّل نفسه"، فالتسجيل الذاتي انتقل إلى POL-SEC-022؛ (ب) قرار #2 نصّ على تخزين السرّ غير القابل للعكس دون سياسة تحمله → POL-SEC-023؛ (ج) POL-SEC-017 يسمّي صراحةً كل بيانات اعتماد الحساب المعطَّل لا "تلك" فقط؛ (د) POL-SEC-021 شرط حالة لا خاصية اختيارية → نمط state | Recommended by dialogue round 3 — converged | factory.yaml ids.ears.patterns؛ ADR-SEC-014؛ ADR-SEC-016؛ OWASP Secrets Management Cheat Sheet |
══════════════════════════════════════════════════════════════════

**تصحيح على القرار #4 (ADR-SEC-034، قرار بشري عند توقّف P1):** بوابة VIEW — مثل بوابة الوحدة — جزء من المسار
البنيوي إلى الإجراء، لا صلاحية إضافية. دور مستهلك أحداث Oracle هو: منح وحدة FIN + منح شاشة القيد + VIEW + CREATE،
ولا شيء غير ذلك. صفّ القرار #4 أعلاه يبقى كما اعتُمد؛ هذا السطر هو الكلمة اللاحقة على جملة "لا منح VIEW" وحدها.
الأثر المُعلن: المُستدعي يقرأ قيود يومية FIN ويبحث فيها، ولا يبلغ أي شاشة أخرى ولا أي وحدة أخرى (POL-SEC-001، POL-SEC-002).
