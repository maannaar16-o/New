/* تطبيق «بطل» — منطق التطبيق */
(function () {
  "use strict";

  // ============ الثوابت ============

  const NUTRIENTS = [
    { key: "kcal",    name: "السعرات",   unit: "سعر",   hasMax: true },
    { key: "protein", name: "البروتين",  unit: "جم" },
    { key: "carbs",   name: "الكربوهيدرات", unit: "جم", optional: true },
    { key: "fiber",   name: "الألياف",   unit: "جم" },
    { key: "ca",      name: "الكالسيوم", unit: "مجم" },
    { key: "vitD",    name: "فيتامين د", unit: "مكجم" },
    { key: "fe",      name: "الحديد",    unit: "مجم" },
    { key: "zn",      name: "الزنك",     unit: "مجم" },
    { key: "vitA",    name: "فيتامين أ", unit: "مكجم" },
    { key: "vitC",    name: "فيتامين ج", unit: "مجم" },
  ];

  // أهداف افتراضية لطفل 12 سنة محدود الحركة (استرشادية)
  const DEFAULT_TARGETS = {
    kcal: 1400, protein: 34, carbs: 180, fiber: 20,
    ca: 1300, vitD: 15, fe: 8, zn: 8, vitA: 600, vitC: 45,
    waterCups: 7,
  };

  const MEALS = [
    { id: "breakfast", name: "الفطار", icon: "🌅" },
    { id: "lunch", name: "الغدا", icon: "☀️" },
    { id: "dinner", name: "العشا", icon: "🌙" },
    { id: "snack", name: "السناك", icon: "🍎" },
  ];

  const GREETINGS = [
    "أهلًا بأم البطل 💪",
    "صباح الخير يا أم فارس البطل 🌞",
    "أهلًا بماما البطلة ❤️",
    "يوم جديد مع بطلنا فارس ⭐",
    "أهلًا بصانعة الأبطال 🏆",
  ];

  const MOTIVATIONS = [
    "كل معلقة أكل بحب هي خطوة في رحلة فارس نحو القوة 🌱",
    "فارس بطل حقيقي، وجلسات علاجه تحتاج طاقة… وأنتِ مصدر الطاقة دي ⚡",
    "الاهتمام الصغير كل يوم يصنع فرقًا كبيرًا مع الوقت 🌟",
    "أنتِ لا تطعمين فارس فقط… أنتِ تبنين عظامه وعضلاته وابتسامته ❤️",
    "لا يوجد يوم مثالي، يوجد أم تحاول كل يوم… وهذا هو البطولة الحقيقية 🏅",
    "جسم فارس يعمل ويكبر، وكل وجبة متوازنة تساعده في جلسات العلاج الطبيعي 💪",
    "الكالسيوم واللبن أصدقاء عظام فارس… استمري! 🦴",
    "نجاح اليوم ليس الكمال، بل المحاولة بحب 🌷",
    "فارس محظوظ بأم مثلك تهتم بكل تفصيلة 🌸",
    "الماء والألياف يريحان بطن فارس ويجعلان يومه أخف 💧",
    "خطوة بخطوة… التغذية السليمة تسند العلاج الطبيعي والحسي 👣",
    "ابتسامة فارس اليوم هي شهادة نجاحك 😊",
    "قوة البطل تبدأ من مطبخ البيت 🍲",
    "أنتِ الطبيبة الأولى في حياة فارس… بحبك واهتمامك 🩺",
    "كل يوم جديد فرصة جديدة… لا تحملي هم الأمس 🌅",
    "التنويع في الأكل يفتح شهية فارس ويكمل عناصره 🥗",
    "راحة الأم مهمة أيضًا… اعتني بنفسك كما تعتنين بفارس 💚",
    "بطلنا يكبر، وكل وجبة صحية هي طوبة في بنيانه 🧱",
    "الحب أهم مكوّن في كل وجبة… وأنتِ خبيرته 💝",
    "فيتامين د والشمس الخفيفة صديقان لفارس ☀️",
    "لو أكل فارس أقل من المعتاد اليوم فلا بأس… غدًا يوم جديد 🌈",
    "الوجبات الصغيرة المتكررة أسهل على فارس من وجبة كبيرة واحدة 🍽️",
    "أنتِ جزء أساسي من فريق علاج فارس… وأهم عضو فيه 🤝",
    "التقدم الصغير كل أسبوع هو انتصار كبير 🎉",
    "صحة فارس مشروع أسرة… وأنتِ قائدته 👑",
    "طعام ملون في الطبق = عناصر متنوعة في الجسم 🌈",
    "بالصبر والحب… فارس يعطي أفضل ما عنده كل يوم 🕊️",
    "قصة فارس قصة بطولة يومية… وأنتِ راويتها وبطلتها معه ✨",
    "لا تقارني فارس إلا بفارس الأمس… وهو يتقدم 📈",
    "دعمك وحنانك غذاء لروح فارس قبل جسده 💞",
  ];

  const TIPS = [
    "الزبادي والجبنة القريش من أفضل مصادر الكالسيوم والبروتين السهلة البلع لفارس.",
    "أضيفي ملعقة عسل أسود للفطار — غنية جدًا بالحديد والكالسيوم.",
    "السردين والسمك من أغنى الأطعمة بفيتامين د المهم لعظام فارس.",
    "لو فارس يعاني من إمساك: زيدي الماء والألياف (خضار، فواكه، عدس، بليلة).",
    "هرس الخضار في الشوربة حيلة ذكية لإدخال عناصر كثيرة بقوام سهل.",
    "ملعقة طحينة على الفول أو السلطة تضيف كالسيوم وحديد وسعرات مفيدة.",
    "تعريض فارس للشمس الصباحية الخفيفة 15 دقيقة يساعد جسمه على إنتاج فيتامين د.",
    "الكبدة مرة في الأسبوع كنز من الحديد وفيتامين أ.",
    "عصير البرتقال أو الجوافة مع الوجبة يساعد على امتصاص الحديد من الطعام.",
    "اللوز المطحون يُضاف للأرز باللبن أو الزبادي — كالسيوم وطاقة إضافية.",
    "قدّمي الأكل الجديد أكثر من مرة — الطفل قد يحتاج 10 محاولات ليتقبل طعمًا جديدًا.",
    "البطاطا المشوية غنية بفيتامين أ المفيد لصحة العين والمناعة.",
    "اجعلي وقت الأكل هادئًا وممتعًا — التوتر يقلل الشهية.",
    "الوجبات الصغيرة كل 3 ساعات أسهل لفارس من 3 وجبات كبيرة.",
    "الموز باللبن مشروب طاقة طبيعي ممتاز قبل جلسة العلاج الطبيعي.",
  ];

  const INFO_CARDS = [
    {
      icon: "🏗️", title: "البروتين — بنّاء العضلات",
      body: "البروتين يبني عضلات فارس ويساعده على الاستفادة من جلسات العلاج الطبيعي.",
      list: ["البيض والفراخ واللحمة والسمك", "العدس والفول والحمص", "الزبادي والجبنة القريش واللبن"],
    },
    {
      icon: "🦴", title: "الكالسيوم وفيتامين د — أصدقاء العظام",
      body: "أطفال الشلل الدماغي الذين لا يمشون معرضون أكثر لضعف العظام، لذا الكالسيوم وفيتامين د أولوية قصوى لفارس.",
      list: ["اللبن والزبادي والجبن بأنواعها", "السردين والسمك (غني بفيتامين د)", "الطحينة والحلاوة واللوز والملوخية", "شمس الصباح الخفيفة تساعد على إنتاج فيتامين د"],
    },
    {
      icon: "💧", title: "الماء والألياف — راحة البطن",
      body: "الإمساك شائع عند أطفال الشلل الدماغي بسبب قلة الحركة. الماء الكافي والألياف يقللانه كثيرًا.",
      list: ["6-8 أكواب ماء يوميًا", "الخضار والفواكه (خاصة الجوافة والكمثرى والبرتقال)", "العدس والفول والبليلة والشوفان", "البلح والزبيب ملينات طبيعية لطيفة"],
    },
    {
      icon: "🩸", title: "الحديد — ضد الأنيميا",
      body: "الحديد يحمي فارس من فقر الدم ويحافظ على تركيزه ونشاطه.",
      list: ["الكبدة واللحمة الحمراء", "العدس والسبانخ والملوخية", "العسل الأسود والطحينة", "فيتامين ج (برتقال/جوافة) مع الوجبة يزيد امتصاص الحديد"],
    },
    {
      icon: "👁️", title: "فيتامين أ — لصحة العين والمناعة",
      body: "مهم لدعم صحة عين فارس ومناعته.",
      list: ["الجزر والبطاطا المشوية", "الكبدة (أغنى مصدر)", "السبانخ والملوخية والمانجو"],
    },
    {
      icon: "🥄", title: "قوام الطعام وسهولة البلع",
      body: "إذا كان المضغ أو البلع صعبًا على فارس أحيانًا، القوام المناسب يجعل الأكل أسهل وأأمن.",
      list: ["الهرس والبوريه والشوربة الكثيفة خيارات ممتازة", "قطّعي الطعام قطعًا صغيرة جدًا", "أطعميه وهو في وضع جلوس معتدل قدر الإمكان", "استشيري أخصائي البلع إذا لاحظتِ كحة متكررة مع الأكل"],
    },
    {
      icon: "⏰", title: "نظام الوجبات",
      body: "وجبات صغيرة متكررة أفضل لفارس من وجبات كبيرة قليلة.",
      list: ["3 وجبات رئيسية + 2-3 سناك", "سناك مغذي: زبادي، موز باللبن، بليلة، تمر", "وجبة خفيفة قبل جلسة العلاج الطبيعي تعطيه طاقة"],
    },
    {
      icon: "🌈", title: "قاعدة الطبق الملون",
      body: "كلما تنوعت ألوان الطعام في اليوم، اكتملت العناصر الغذائية.",
      list: ["أخضر: خضار ورقية", "برتقالي: جزر وبطاطا ومشمش", "أحمر: طماطم وفراولة", "أبيض: لبن وزبادي وفراخ"],
    },
  ];

  // أسماء العناصر التي نقترح أطعمة لتعويضها + صياغة الاقتراح
  const SUGGEST_NUTRIENTS = ["protein", "fiber", "ca", "vitD", "fe", "zn", "vitA", "vitC"];

  // ============ التخزين ============

  const store = {
    get(key, fallback) {
      try {
        const raw = localStorage.getItem(key);
        return raw ? JSON.parse(raw) : fallback;
      } catch (e) { return fallback; }
    },
    set(key, value) {
      try { localStorage.setItem(key, JSON.stringify(value)); } catch (e) { /* مساحة ممتلئة */ }
    },
    remove(key) { try { localStorage.removeItem(key); } catch (e) {} },
  };

  function todayKey(offsetDays = 0) {
    const d = new Date();
    d.setDate(d.getDate() + offsetDays);
    const y = d.getFullYear();
    const m = String(d.getMonth() + 1).padStart(2, "0");
    const day = String(d.getDate()).padStart(2, "0");
    return `${y}-${m}-${day}`;
  }

  function emptyDay() {
    return { meals: { breakfast: [], lunch: [], dinner: [], snack: [] }, waterCups: 0 };
  }

  function loadDay(key) {
    const d = store.get("faris:log:" + key, null);
    if (!d) return emptyDay();
    if (!d.meals) d.meals = emptyDay().meals;
    MEALS.forEach((m) => { if (!Array.isArray(d.meals[m.id])) d.meals[m.id] = []; });
    if (typeof d.waterCups !== "number") d.waterCups = 0;
    return d;
  }

  function saveDay(key, data) { store.set("faris:log:" + key, data); }

  function loadTargets() {
    return Object.assign({}, DEFAULT_TARGETS, store.get("faris:targets", {}));
  }

  // ============ الحالة ============

  let targets = loadTargets();
  let day = loadDay(todayKey());
  let currentMeal = "breakfast";
  let pickerFood = null;      // الصنف المختار في النافذة
  let pickerCat = "all";

  // ============ أدوات ============

  const $ = (sel) => document.querySelector(sel);
  const $$ = (sel) => Array.from(document.querySelectorAll(sel));

  function el(tag, cls, html) {
    const e = document.createElement(tag);
    if (cls) e.className = cls;
    if (html !== undefined) e.innerHTML = html;
    return e;
  }

  function fmt(n) {
    if (n >= 100) return String(Math.round(n));
    if (n >= 10) return String(Math.round(n * 10) / 10);
    return String(Math.round(n * 10) / 10);
  }

  function dayOfYear() {
    const now = new Date();
    return Math.floor((now - new Date(now.getFullYear(), 0, 0)) / 86400000);
  }

  let toastTimer = null;
  function toast(msg) {
    const t = $("#toast");
    t.textContent = msg;
    t.hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => { t.hidden = true; }, 2200);
  }

  // ============ الحسابات ============

  function entryNutrients(entry) {
    const food = FOODS.find((f) => f.id === entry.foodId);
    if (!food) return null;
    const factor = entry.grams / 100;
    const out = {};
    NUTRIENTS.forEach((n) => { out[n.key] = (food[n.key] || 0) * factor; });
    return out;
  }

  function sumDay(dayData) {
    const totals = {};
    NUTRIENTS.forEach((n) => { totals[n.key] = 0; });
    MEALS.forEach((m) => {
      (dayData.meals[m.id] || []).forEach((entry) => {
        const nut = entryNutrients(entry);
        if (nut) NUTRIENTS.forEach((n) => { totals[n.key] += nut[n.key]; });
      });
    });
    return totals;
  }

  function sumMeal(entries) {
    const totals = {};
    NUTRIENTS.forEach((n) => { totals[n.key] = 0; });
    entries.forEach((entry) => {
      const nut = entryNutrients(entry);
      if (nut) NUTRIENTS.forEach((n) => { totals[n.key] += nut[n.key]; });
    });
    return totals;
  }

  function ratioStatus(ratio) {
    if (ratio >= 0.9) return "ok";
    if (ratio >= 0.6) return "near";
    return "low";
  }

  // ============ الرئيسية ============

  function renderGreeting() {
    $("#greeting").textContent = GREETINGS[dayOfYear() % GREETINGS.length];
    const d = new Date();
    $("#today-date").textContent = d.toLocaleDateString("ar-EG", {
      weekday: "long", day: "numeric", month: "long", year: "numeric",
    });
  }

  function renderMotivation() {
    $("#motivation-msg").textContent = MOTIVATIONS[dayOfYear() % MOTIVATIONS.length];
    $("#daily-tip").textContent = TIPS[dayOfYear() % TIPS.length];
  }

  function ringSVG(ratio, color) {
    const r = 30, c = 2 * Math.PI * r;
    const pct = Math.min(ratio, 1);
    const offset = c * (1 - pct);
    return `<svg class="ring-svg" viewBox="0 0 72 72">
      <circle cx="36" cy="36" r="${r}" fill="none" stroke="var(--border)" stroke-width="7"/>
      <circle cx="36" cy="36" r="${r}" fill="none" stroke="${color}" stroke-width="7"
        stroke-linecap="round" stroke-dasharray="${c}" stroke-dashoffset="${offset}"
        transform="rotate(-90 36 36)"/>
      <text x="36" y="41" text-anchor="middle" font-size="16" font-weight="800" fill="var(--text)">${Math.round(ratio * 100)}%</text>
    </svg>`;
  }

  const HOME_RING_KEYS = ["kcal", "protein", "ca", "fiber"];
  const STATUS_COLORS = { ok: "var(--ok)", near: "var(--near)", low: "var(--low)" };

  function renderHomeRings() {
    const totals = sumDay(day);
    const wrap = $("#home-rings");
    wrap.innerHTML = "";
    HOME_RING_KEYS.forEach((key) => {
      const n = NUTRIENTS.find((x) => x.key === key);
      const target = targets[key] || 1;
      const ratio = totals[key] / target;
      const item = el("div", "ring-item");
      item.innerHTML = ringSVG(ratio, STATUS_COLORS[ratioStatus(ratio)]) +
        `<div class="ring-label">${n.name}</div>` +
        `<div class="ring-sub">${fmt(totals[key])} / ${fmt(target)} ${n.unit}</div>`;
      wrap.appendChild(item);
    });
  }

  function renderWater() {
    const target = targets.waterCups || 7;
    const wrap = $("#water-cups");
    wrap.innerHTML = "";
    const shown = Math.max(target, day.waterCups);
    for (let i = 0; i < shown; i++) {
      const cup = el("span", "water-cup" + (i < day.waterCups ? " full" : ""), "🥤");
      wrap.appendChild(cup);
    }
    $("#water-count").textContent = `${day.waterCups} / ${target}`;
  }

  // ============ الوجبات ============

  function renderMealTabs() {
    $$(".meal-tab").forEach((btn) => {
      btn.classList.toggle("active", btn.dataset.meal === currentMeal);
    });
  }

  function totalsChips(totals, keys) {
    return keys.map((key) => {
      const n = NUTRIENTS.find((x) => x.key === key);
      return `<div class="total-chip"><b>${fmt(totals[key])}</b><span>${n.name} (${n.unit})</span></div>`;
    }).join("");
  }

  function renderMealItems() {
    const entries = day.meals[currentMeal];
    const wrap = $("#meal-items");
    wrap.innerHTML = "";
    entries.forEach((entry, idx) => {
      const food = FOODS.find((f) => f.id === entry.foodId);
      if (!food) return;
      const nut = entryNutrients(entry);
      const item = el("div", "meal-item");
      item.innerHTML =
        `<div class="meal-item-info">
           <div class="meal-item-name">${food.name}</div>
           <div class="meal-item-qty">${entry.qtyLabel} (${fmt(entry.grams)} جم)</div>
         </div>
         <div class="meal-item-kcal">${fmt(nut.kcal)} سعر</div>`;
      const del = el("button", "meal-item-del", "🗑️");
      del.setAttribute("aria-label", "حذف " + food.name);
      del.addEventListener("click", () => {
        entries.splice(idx, 1);
        persistAndRender();
        toast("تم حذف الصنف");
      });
      item.appendChild(del);
      wrap.appendChild(item);
    });

    $("#meal-totals").innerHTML = entries.length
      ? totalsChips(sumMeal(entries), ["kcal", "protein", "ca", "fiber"])
      : "";
    $("#day-totals").innerHTML = totalsChips(sumDay(day), ["kcal", "protein", "ca", "fiber"]);
  }

  // ============ نافذة اختيار الصنف ============

  function openFoodModal() {
    pickerFood = null;
    $("#food-modal").hidden = false;
    $("#food-picker-step").hidden = false;
    $("#food-qty-step").hidden = true;
    const meal = MEALS.find((m) => m.id === currentMeal);
    $("#food-modal-title").textContent = `أضيفي صنف لـ${meal.name} ${meal.icon}`;
    $("#food-search").value = "";
    renderCatChips();
    renderFoodList();
  }

  function closeFoodModal() { $("#food-modal").hidden = true; }

  function renderCatChips() {
    const wrap = $("#food-cat-chips");
    wrap.innerHTML = "";
    const all = [{ id: "all", name: "الكل" }].concat(FOOD_CATEGORIES);
    all.forEach((cat) => {
      const chip = el("button", "cat-chip" + (pickerCat === cat.id ? " active" : ""), cat.name);
      chip.type = "button";
      chip.addEventListener("click", () => {
        pickerCat = cat.id;
        renderCatChips();
        renderFoodList();
      });
      wrap.appendChild(chip);
    });
  }

  function renderFoodList() {
    const q = $("#food-search").value.trim();
    const list = $("#food-list");
    list.innerHTML = "";
    FOODS
      .filter((f) => (pickerCat === "all" || f.cat === pickerCat) && (!q || f.name.includes(q)))
      .forEach((f) => {
        const li = el("li");
        li.innerHTML = `<span class="fl-name">${f.name}</span><span class="fl-kcal">${f.kcal} سعر/100جم</span>`;
        li.addEventListener("click", () => startQtyStep(f));
        list.appendChild(li);
      });
    if (!list.children.length) {
      list.appendChild(el("li", "", `<span class="muted">لا توجد نتائج — جرّبي كلمة أخرى</span>`));
    }
  }

  function startQtyStep(food) {
    pickerFood = food;
    $("#food-picker-step").hidden = true;
    $("#food-qty-step").hidden = false;
    $("#qty-food-name").textContent = food.name;
    const sel = $("#qty-unit");
    sel.innerHTML = "";
    food.units.forEach((u, i) => {
      const opt = el("option", "", `${u.name} (${u.grams} جم)`);
      opt.value = String(i);
      sel.appendChild(opt);
    });
    const gOpt = el("option", "", "جرامات (إدخال يدوي)");
    gOpt.value = "grams";
    sel.appendChild(gOpt);
    sel.value = "0";
    $("#qty-amount").value = "1";
    updateQtyPreview();
  }

  function currentQtyGrams() {
    const sel = $("#qty-unit").value;
    const amount = parseFloat($("#qty-amount").value) || 0;
    if (sel === "grams") return { grams: amount, label: `${fmt(amount)} جم` };
    const unit = pickerFood.units[parseInt(sel, 10)];
    return { grams: amount * unit.grams, label: `${fmt(amount)} × ${unit.name}` };
  }

  function updateQtyPreview() {
    if (!pickerFood) return;
    const { grams } = currentQtyGrams();
    const factor = grams / 100;
    const kcal = pickerFood.kcal * factor;
    const protein = pickerFood.protein * factor;
    const ca = pickerFood.ca * factor;
    $("#qty-preview").innerHTML =
      `${fmt(grams)} جم ≈ <b>${fmt(kcal)}</b> سعر • ` +
      `بروتين <b>${fmt(protein)}</b> جم • كالسيوم <b>${fmt(ca)}</b> مجم`;
  }

  function confirmAddFood() {
    if (!pickerFood) return;
    const { grams, label } = currentQtyGrams();
    if (!grams || grams <= 0) { toast("أدخلي كمية صحيحة"); return; }
    day.meals[currentMeal].push({ foodId: pickerFood.id, grams, qtyLabel: label });
    persistAndRender();
    closeFoodModal();
    toast(`تمت إضافة ${pickerFood.name} ✅`);
  }

  // ============ التقييم ============

  function foodSuggestions(nutrientKey, count = 3) {
    // أفضل الأصناف في العنصر الناقص (مع استبعاد الزيوت للسعرات المعقولة)
    return FOODS
      .slice()
      .sort((a, b) => (b[nutrientKey] || 0) - (a[nutrientKey] || 0))
      .filter((f) => (f[nutrientKey] || 0) > 0)
      .slice(0, count);
  }

  function renderReview() {
    const totals = sumDay(day);
    const hasFood = MEALS.some((m) => day.meals[m.id].length > 0);

    const summary = $("#review-summary");
    const bars = $("#review-bars");
    const sugWrap = $("#review-suggestions");
    bars.innerHTML = "";
    sugWrap.innerHTML = "";

    if (!hasFood) {
      summary.innerHTML = `<p class="review-status">لم تُسجَّل وجبات اليوم بعد 🍽️</p>
        <p class="muted">سجّلي وجبات فارس أولًا من شاشة «الوجبات» ثم عودي هنا لرؤية التقييم.</p>`;
      return;
    }

    // الأشرطة
    let okCount = 0, lowList = [];
    const reviewKeys = NUTRIENTS.filter((n) => !n.optional);
    reviewKeys.forEach((n) => {
      const target = targets[n.key] || 1;
      const ratio = totals[n.key] / target;
      const status = ratioStatus(ratio);
      if (status === "ok") okCount++;
      if (status === "low") lowList.push(n);

      let statusText, statusIcon;
      if (n.hasMax && ratio > 1.2) {
        statusText = "أعلى من الهدف قليلًا — لا بأس، انتبهي فقط للحلويات";
        statusIcon = "🟠";
      } else if (status === "ok") { statusText = "ممتاز! تحقق الهدف"; statusIcon = "✅"; }
      else if (status === "near") { statusText = "قريب من الهدف — ينقصه القليل"; statusIcon = "🟡"; }
      else { statusText = "يحتاج المزيد اليوم"; statusIcon = "🔴"; }

      const card = el("div", "card nutrient-bar-card");
      card.innerHTML =
        `<div class="nb-head">
           <span class="nb-name">${n.name}</span>
           <span class="nb-value">${fmt(totals[n.key])} / ${fmt(target)} ${n.unit}</span>
         </div>
         <div class="nb-track"><div class="nb-fill ${status}" style="width:${Math.min(ratio * 100, 100)}%"></div></div>
         <div class="nb-status">${statusIcon} ${statusText}</div>`;
      bars.appendChild(card);
    });

    // الملخص
    const total = reviewKeys.length;
    let headline, sub;
    if (okCount >= total - 1) {
      headline = "🏆 يوم رائع! تغذية فارس اليوم ممتازة";
      sub = "أنتِ فعلًا أم بطلة… استمري على نفس النهج 👏";
    } else if (okCount >= Math.ceil(total / 2)) {
      headline = "👍 يوم جيد — فارس أخذ معظم احتياجاته";
      sub = "خطوات بسيطة وتكتمل الصورة. شوفي الاقتراحات بالأسفل.";
    } else {
      headline = "🌱 البداية موجودة — فارس يحتاج المزيد اليوم";
      sub = "لا تقلقي، الاقتراحات بالأسفل تساعدك تكملي احتياجات فارس بسهولة.";
    }
    summary.innerHTML = `<p class="review-status">${headline}</p><p class="muted">${sub}</p>`;

    // اقتراحات للعناصر الناقصة
    lowList
      .filter((n) => SUGGEST_NUTRIENTS.includes(n.key))
      .slice(0, 4)
      .forEach((n) => {
        const foods = foodSuggestions(n.key).map((f) => f.name).join("، ");
        const card = el("div", "card suggest-card");
        card.innerHTML = `<h3>فارس يحتاج ${n.name} 💡</h3>
          <p>جرّبي: <b>${foods}</b></p>`;
        sugWrap.appendChild(card);
      });

    // الماء
    const waterTarget = targets.waterCups || 7;
    if (day.waterCups < waterTarget) {
      const card = el("div", "card suggest-card");
      card.innerHTML = `<h3>💧 الماء</h3><p>شرب فارس <b>${day.waterCups}</b> من <b>${waterTarget}</b> أكواب — كوب إضافي يساعد الهضم ويمنع الإمساك.</p>`;
      sugWrap.appendChild(card);
    }
  }

  // ============ السجل الأسبوعي ============

  const WEEK_KEYS = ["kcal", "protein", "ca", "fiber"];

  function renderWeek() {
    const daysWrap = $("#week-days");
    const summaryWrap = $("#week-summary");
    daysWrap.innerHTML = "";

    const achieved = {}; // عدد الأيام التي تحقق فيها كل عنصر
    WEEK_KEYS.forEach((k) => { achieved[k] = 0; });
    let daysWithData = 0;

    for (let i = 0; i < 7; i++) {
      const key = todayKey(-i);
      const d = i === 0 ? day : loadDay(key);
      const hasFood = MEALS.some((m) => (d.meals[m.id] || []).length > 0);
      const totals = sumDay(d);

      const date = new Date();
      date.setDate(date.getDate() - i);
      const dayName = i === 0 ? "اليوم" : i === 1 ? "أمس" :
        date.toLocaleDateString("ar-EG", { weekday: "long" });
      const dateStr = date.toLocaleDateString("ar-EG", { day: "numeric", month: "short" });

      const card = el("div", "card week-day-card");
      if (!hasFood) {
        card.innerHTML = `<div class="wd-head"><span>${dayName} · ${dateStr}</span><span class="muted">لا توجد بيانات</span></div>`;
        daysWrap.appendChild(card);
        continue;
      }
      daysWithData++;

      let okCount = 0;
      const barsHTML = WEEK_KEYS.map((k) => {
        const n = NUTRIENTS.find((x) => x.key === k);
        const ratio = totals[k] / (targets[k] || 1);
        const status = ratioStatus(ratio);
        if (status === "ok") { okCount++; achieved[k]++; }
        return `<div class="wd-bar-item">
          <div class="wd-mini-track"><div class="wd-mini-fill" style="width:${Math.min(ratio * 100, 100)}%;background:${STATUS_COLORS[status]}"></div></div>
          ${n.name}
        </div>`;
      }).join("");

      card.innerHTML =
        `<div class="wd-head"><span>${dayName} · ${dateStr}</span><span class="wd-score">${okCount}/${WEEK_KEYS.length} ✅</span></div>
         <div class="wd-bars">${barsHTML}</div>`;
      daysWrap.appendChild(card);
    }

    if (!daysWithData) {
      summaryWrap.innerHTML = `<p class="muted">سجّلي وجبات فارس يوميًا وسيظهر هنا ملخص أسبوعي يمكنك عرضه على أخصائي التغذية.</p>`;
      return;
    }
    const lines = WEEK_KEYS.map((k) => {
      const n = NUTRIENTS.find((x) => x.key === k);
      return `<li><b>${n.name}</b>: تحقق في ${achieved[k]} من ${daysWithData} ${daysWithData > 2 ? "أيام" : "يوم"}</li>`;
    }).join("");
    summaryWrap.innerHTML =
      `<p class="review-status">ملخص الأسبوع 📅</p>
       <ul style="padding-right:20px">${lines}</ul>
       <p class="muted">هذا الملخص مفيد لعرضه على طبيب فارس أو أخصائي التغذية.</p>`;
  }

  // ============ معلومات ============

  function renderInfo() {
    const wrap = $("#info-cards");
    wrap.innerHTML = "";
    INFO_CARDS.forEach((c) => {
      const card = el("div", "card info-card");
      card.innerHTML = `<h3>${c.icon} ${c.title}</h3><p>${c.body}</p>` +
        (c.list ? `<ul>${c.list.map((li) => `<li>${li}</li>`).join("")}</ul>` : "");
      wrap.appendChild(card);
    });
  }

  // ============ الإعدادات ============

  const TARGET_FIELDS = [
    { key: "kcal", name: "السعرات الحرارية", unit: "سعر" },
    { key: "protein", name: "البروتين", unit: "جم" },
    { key: "carbs", name: "الكربوهيدرات", unit: "جم" },
    { key: "fiber", name: "الألياف", unit: "جم" },
    { key: "ca", name: "الكالسيوم", unit: "مجم" },
    { key: "vitD", name: "فيتامين د", unit: "مكجم" },
    { key: "fe", name: "الحديد", unit: "مجم" },
    { key: "zn", name: "الزنك", unit: "مجم" },
    { key: "vitA", name: "فيتامين أ", unit: "مكجم" },
    { key: "vitC", name: "فيتامين ج", unit: "مجم" },
    { key: "waterCups", name: "أكواب الماء", unit: "كوب" },
  ];

  function renderTargetsForm() {
    const form = $("#targets-form");
    form.innerHTML = "";
    TARGET_FIELDS.forEach((f) => {
      const row = el("div", "target-field");
      row.innerHTML =
        `<label for="t-${f.key}">${f.name}</label>
         <input type="number" id="t-${f.key}" value="${targets[f.key]}" min="0" step="any" inputmode="decimal">
         <span class="unit">${f.unit}</span>`;
      form.appendChild(row);
    });
  }

  function saveTargets() {
    const next = {};
    let valid = true;
    TARGET_FIELDS.forEach((f) => {
      const v = parseFloat($("#t-" + f.key).value);
      if (isNaN(v) || v <= 0) { valid = false; return; }
      next[f.key] = v;
    });
    if (!valid) { toast("تأكدي أن كل القيم أرقام أكبر من صفر"); return; }
    targets = next;
    store.set("faris:targets", targets);
    toast("تم حفظ الأهداف ✅");
    renderAll();
  }

  // ============ التنقل ============

  function showScreen(id) {
    $$(".screen").forEach((s) => s.classList.toggle("active", s.id === "screen-" + id));
    $$(".nav-btn").forEach((b) => b.classList.toggle("active", b.dataset.screen === id));
    // تحديث الشاشات الديناميكية عند فتحها
    if (id === "home") { renderHomeRings(); renderWater(); }
    if (id === "log") { renderMealTabs(); renderMealItems(); }
    if (id === "review") renderReview();
    if (id === "week") renderWeek();
    window.scrollTo({ top: 0 });
  }

  function persistAndRender() {
    saveDay(todayKey(), day);
    renderAll();
  }

  function renderAll() {
    renderHomeRings();
    renderWater();
    renderMealTabs();
    renderMealItems();
    renderReview();
    renderWeek();
  }

  // ============ ربط الأحداث ============

  function bindEvents() {
    // التنقل
    $$(".nav-btn").forEach((b) => b.addEventListener("click", () => showScreen(b.dataset.screen)));
    $$("[data-goto]").forEach((b) => b.addEventListener("click", () => showScreen(b.dataset.goto)));

    // الماء
    $("#water-plus").addEventListener("click", () => {
      day.waterCups++;
      persistAndRender();
      if (day.waterCups === (targets.waterCups || 7)) toast("🎉 فارس شرب كفايته من الماء اليوم!");
    });
    $("#water-minus").addEventListener("click", () => {
      if (day.waterCups > 0) { day.waterCups--; persistAndRender(); }
    });

    // تبويبات الوجبات
    $$(".meal-tab").forEach((b) => b.addEventListener("click", () => {
      currentMeal = b.dataset.meal;
      renderMealTabs();
      renderMealItems();
    }));

    // نافذة الصنف
    $("#add-food-btn").addEventListener("click", openFoodModal);
    $("#food-modal-close").addEventListener("click", closeFoodModal);
    $("#food-modal").addEventListener("click", (e) => { if (e.target.id === "food-modal") closeFoodModal(); });
    $("#food-search").addEventListener("input", renderFoodList);
    $("#qty-unit").addEventListener("change", updateQtyPreview);
    $("#qty-amount").addEventListener("input", updateQtyPreview);
    $("#qty-plus").addEventListener("click", () => {
      const inp = $("#qty-amount");
      inp.value = String((parseFloat(inp.value) || 0) + (parseFloat(inp.value) >= 1 ? 0.5 : 0.25));
      updateQtyPreview();
    });
    $("#qty-minus").addEventListener("click", () => {
      const inp = $("#qty-amount");
      const next = (parseFloat(inp.value) || 0) - 0.5;
      inp.value = String(Math.max(next, 0.25));
      updateQtyPreview();
    });
    $("#qty-confirm").addEventListener("click", confirmAddFood);
    $("#qty-back").addEventListener("click", () => {
      $("#food-qty-step").hidden = true;
      $("#food-picker-step").hidden = false;
    });

    // الإعدادات
    $("#save-targets").addEventListener("click", saveTargets);
    $("#reset-targets").addEventListener("click", () => {
      targets = Object.assign({}, DEFAULT_TARGETS);
      store.set("faris:targets", targets);
      renderTargetsForm();
      renderAll();
      toast("تمت استعادة القيم الافتراضية");
    });
    $("#clear-today").addEventListener("click", () => {
      if (confirm("هل تريدين مسح كل بيانات اليوم الحالي (الوجبات والماء)؟")) {
        day = emptyDay();
        store.remove("faris:log:" + todayKey());
        renderAll();
        toast("تم مسح بيانات اليوم");
      }
    });
  }

  // ============ التشغيل ============

  function init() {
    renderGreeting();
    renderMotivation();
    renderInfo();
    renderTargetsForm();
    bindEvents();
    renderAll();

    // تسجيل service worker
    if ("serviceWorker" in navigator) {
      navigator.serviceWorker.register("sw.js").catch(() => {});
    }
  }

  document.addEventListener("DOMContentLoaded", init);
})();
