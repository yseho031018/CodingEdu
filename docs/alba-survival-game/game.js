"use strict";

const canvas = document.getElementById("gameCanvas");
const ctx = canvas.getContext("2d");
const toast = document.getElementById("toast");
const phone = document.getElementById("phone");
const commutePanel = document.getElementById("commutePanel");
const schedulePanel = document.getElementById("schedulePanel");
const shopPanel = document.getElementById("shopPanel");
const sceneBadge = document.getElementById("sceneBadge");
const sideTitle = document.getElementById("sideTitle");
const questList = document.getElementById("questList");
const memoryPanel = document.getElementById("memoryPanel");
const savingsText = document.getElementById("savingsText");
const savingsBar = document.getElementById("savingsBar");
const mentalValue = document.getElementById("mentalValue");
const mentalBar = document.getElementById("mentalBar");
const staminaValue = document.getElementById("staminaValue");
const staminaBar = document.getElementById("staminaBar");
const dayTimeText = document.getElementById("dayTimeText");
const shiftPlaceText = document.getElementById("shiftPlaceText");
const nextWorkText = document.getElementById("nextWorkText");
const actionHint = document.getElementById("actionHint");
const logList = document.getElementById("logList");
const summaryList = document.getElementById("summaryList");
const inventoryList = document.getElementById("inventoryList");
const scaleShell = document.querySelector(".scale-shell");
const gameFrame = document.querySelector(".game-frame");

const W = canvas.width;
const H = canvas.height;
const GOAL = 3000000;
const RENT_TOTAL = 628000;
const WORK_PASS_RATE = 70;
const WEEK_MINUTES = 7 * 1440;
const STOCK_TRADE_AMOUNT = 10000;
const keys = new Set();

ctx.imageSmoothingEnabled = false;

const bgImages = {
  home: image("./assets/room-scene-clean-v2.png?v=1"),
  store: image("./assets/store-scene-clean-v2.png?v=1"),
  cafe: image("./assets/cafe-scene-v1.png?v=1"),
  pcbang: image("./assets/pcbang-scene-v1.png?v=1"),
  restaurant: image("./assets/restaurant-scene-v1.png?v=1"),
};

const playerImg = image("./assets/player-character.png?v=3");

const dayNames = ["월", "화", "수", "목", "금", "토", "일"];

const weeklySchedule = [
  { dayIndex: 0, start: 21 * 60, duration: 330, period: "야간", place: "편의점", scene: "store", pay: 61920 },
  { dayIndex: 1, start: 15 * 60, duration: 300, period: "오후", place: "카페", scene: "cafe", pay: 50150 },
  { dayIndex: 2, start: 0, duration: 0, period: "휴무", place: "자취방", scene: "home", pay: 0 },
  { dayIndex: 3, start: 9 * 60, duration: 300, period: "오전", place: "카페", scene: "cafe", pay: 50150 },
  { dayIndex: 4, start: 22 * 60, duration: 330, period: "야간", place: "PC방", scene: "pcbang", pay: 62200 },
  { dayIndex: 5, start: 17 * 60, duration: 360, period: "오후", place: "음식점", scene: "restaurant", pay: 66180 },
  { dayIndex: 6, start: 0, duration: 0, period: "휴무", place: "자취방", scene: "home", pay: 0 },
];

const commuteOptions = {
  transit: {
    label: "대중교통",
    icon: "버스",
    cost: 1450,
    minutes: 35,
    effect: { stamina: -3, mental: -1 },
    note: "싸지만 시간이 오래 걸린다. 늦게 나가면 지각 위험이 있다.",
  },
  taxi: {
    label: "택시",
    icon: "택시",
    cost: 12000,
    minutes: 12,
    effect: { stamina: 0, mental: 1 },
    note: "비싸지만 빠르다. 야간 출근 직전에는 꽤 든든하다.",
  },
};

const shopItems = [
  { id: "ramen", name: "컵라면", icon: "🍜", price: 1800, desc: "퇴근 후 바로 먹는 비상식량" },
  { id: "water", name: "생수", icon: "💧", price: 900, desc: "야간 알바 뒤 탈수 방지용" },
  { id: "rice", name: "즉석밥", icon: "🍚", price: 2200, desc: "반찬만 있으면 한 끼 해결" },
  { id: "energy", name: "에너지드링크", icon: "🥤", price: 2500, desc: "졸릴 때 버티는 마지막 카드" },
  { id: "trashbag", name: "종량제봉투", icon: "🗑️", price: 2000, desc: "방치하면 자취방 평점 하락" },
  { id: "tissue", name: "휴지", icon: "🧻", price: 4500, desc: "없으면 삶의 질이 바로 무너짐" },
  { id: "detergent", name: "세탁세제", icon: "🧴", price: 6900, desc: "근무복 빨래 필수템" },
  { id: "wipes", name: "물티슈", icon: "🧽", price: 3200, desc: "책상과 바닥 급한 청소용" },
  { id: "shampoo", name: "샴푸", icon: "🫧", price: 7500, desc: "면접 전날 급하게 찾게 되는 것" },
  { id: "eggs", name: "계란", icon: "🥚", price: 6000, desc: "자취생 단백질 현실템" },
];

const mealItemEffects = {
  컵라면: { effect: { mental: 4, stamina: 7 }, message: "컵라면으로 뜨끈하게 한 끼를 해결했다." },
  생수: { effect: { mental: 1, stamina: 4 }, message: "생수를 마셔서 몸이 조금 가벼워졌다." },
  즉석밥: { effect: { mental: 3, stamina: 8 }, message: "즉석밥으로 든든하게 배를 채웠다." },
  에너지드링크: { effect: { mental: 2, stamina: 6 }, message: "에너지드링크로 잠깐 버틸 힘을 얻었다." },
  계란: { effect: { mental: 3, stamina: 7 }, message: "계란으로 단백질을 챙겼다." },
};

const stocks = [
  { id: "kr-bank", market: "domestic", marketName: "국내", name: "민트은행", price: 15800, basePrice: 15800, change: 0.2, owned: 0, avg: 0, volatility: 0.014 },
  { id: "kr-food", market: "domestic", marketName: "국내", name: "한끼푸드", price: 22400, basePrice: 22400, change: -0.3, owned: 0, avg: 0, volatility: 0.017 },
  { id: "kr-cafe", market: "domestic", marketName: "국내", name: "카페로봇", price: 31700, basePrice: 31700, change: 0.9, owned: 0, avg: 0, volatility: 0.026 },
  { id: "kr-bio", market: "domestic", marketName: "국내", name: "새벽바이오", price: 18900, basePrice: 18900, change: 2.1, owned: 0, avg: 0, volatility: 0.038 },
  { id: "kr-webtoon", market: "domestic", marketName: "국내", name: "웹툰스튜디오", price: 35400, basePrice: 35400, change: -1.0, owned: 0, avg: 0, volatility: 0.029 },
  { id: "kr-cosmetic", market: "domestic", marketName: "국내", name: "루미화장품", price: 46700, basePrice: 46700, change: 0.4, owned: 0, avg: 0, volatility: 0.021 },
  { id: "kr-ship", market: "domestic", marketName: "국내", name: "대한조선", price: 12900, basePrice: 12900, change: -0.8, owned: 0, avg: 0, volatility: 0.023 },
  { id: "kr-convenience", market: "domestic", marketName: "국내", name: "24시리테일", price: 58200, basePrice: 58200, change: 0.5, owned: 0, avg: 0, volatility: 0.016 },
  { id: "us-cloud", market: "overseas", marketName: "해외", name: "Cloud Forge", price: 214000, basePrice: 214000, change: 0.7, owned: 0, avg: 0, volatility: 0.022 },
  { id: "us-chip", market: "overseas", marketName: "해외", name: "Blue Chip Labs", price: 331000, basePrice: 331000, change: 1.6, owned: 0, avg: 0, volatility: 0.03 },
  { id: "us-stream", market: "overseas", marketName: "해외", name: "StreamBox", price: 118000, basePrice: 118000, change: -0.5, owned: 0, avg: 0, volatility: 0.024 },
  { id: "us-burger", market: "overseas", marketName: "해외", name: "Burger Planet", price: 89200, basePrice: 89200, change: 0.1, owned: 0, avg: 0, volatility: 0.013 },
  { id: "us-health", market: "overseas", marketName: "해외", name: "HealthPlus", price: 154000, basePrice: 154000, change: 0.4, owned: 0, avg: 0, volatility: 0.017 },
  { id: "us-space", market: "overseas", marketName: "해외", name: "Orbit Systems", price: 247000, basePrice: 247000, change: -1.2, owned: 0, avg: 0, volatility: 0.036 },
  { id: "us-coffee", market: "overseas", marketName: "해외", name: "Bean Bros", price: 72600, basePrice: 72600, change: 0.6, owned: 0, avg: 0, volatility: 0.019 },
  { id: "us-dividend", market: "overseas", marketName: "해외", name: "Dividend ETF", price: 54200, basePrice: 54200, change: 0.2, owned: 0, avg: 0, volatility: 0.009 },
  { id: "kr-chip", market: "domestic", marketName: "국내", name: "삼전반도체", price: 73400, basePrice: 73400, change: 0.8, owned: 0, avg: 0, volatility: 0.018 },
  { id: "kr-battery", market: "domestic", marketName: "국내", name: "한빛배터리", price: 42100, basePrice: 42100, change: -0.6, owned: 0, avg: 0, volatility: 0.024 },
  { id: "kr-game", market: "domestic", marketName: "국내", name: "네온게임즈", price: 28600, basePrice: 28600, change: 1.4, owned: 0, avg: 0, volatility: 0.032 },
  { id: "us-tech", market: "overseas", marketName: "해외", name: "Nova AI", price: 182500, basePrice: 182500, change: 1.1, owned: 0, avg: 0, volatility: 0.026 },
  { id: "us-ev", market: "overseas", marketName: "해외", name: "Volt Motors", price: 96300, basePrice: 96300, change: -0.4, owned: 0, avg: 0, volatility: 0.03 },
  { id: "us-index", market: "overseas", marketName: "해외", name: "US 500 ETF", price: 68500, basePrice: 68500, change: 0.3, owned: 0, avg: 0, volatility: 0.012 },
];

const doorHotspot = rect(68, 44, 168, 230);
const doorInteractArea = rect(50, 120, 230, 220);
const clockOutHotspot = rect(24, 590, 178, 50);
const clockOutTask = createTask({
  id: "clockOut",
  name: "퇴근 정산",
  icon: "정산",
  x: 94,
  y: 620,
  target: 1,
  minutes: 0,
  effect: {},
  message: "오늘 근무를 정산했다.",
  kind: "clockOut",
});

const randomEvents = [
  {
    jobs: ["store", "cafe", "pcbang", "restaurant"],
    title: "카드 결제 오류",
    message: "단말기가 늦게 반응해서 줄이 잠깐 길어졌다.",
    minutes: 5,
    effect: { mental: -4, customer: -1 },
  },
  {
    jobs: ["store", "pcbang"],
    title: "분리수거 봉투 부족",
    message: "창고에서 새 봉투를 찾느라 시간이 조금 더 걸렸다.",
    minutes: 6,
    effect: { stamina: -3, boss: -1 },
  },
  {
    jobs: ["store"],
    title: "진열 가격표 누락",
    message: "라면 코너 가격표가 빠져 있어 다시 붙였다.",
    minutes: 7,
    effect: { stamina: -2, boss: 1 },
  },
  {
    jobs: ["cafe"],
    title: "얼음 부족",
    message: "아이스 주문이 몰려 제빙기 앞을 여러 번 왔다 갔다 했다.",
    minutes: 8,
    effect: { stamina: -3, customer: -1 },
  },
  {
    jobs: ["pcbang"],
    title: "좌석 민원",
    message: "헤드셋 소리가 안 난다는 손님을 도와줬다.",
    minutes: 7,
    effect: { mental: -2, customer: 1 },
  },
  {
    jobs: ["restaurant"],
    title: "테이블 회전 압박",
    message: "손님이 몰려 빈 그릇과 물컵을 빠르게 치웠다.",
    minutes: 9,
    effect: { stamina: -4, boss: 1 },
  },
  {
    jobs: ["store", "cafe", "pcbang", "restaurant"],
    title: "인수인계 메모 누락",
    message: "앞 근무자가 빠뜨린 내용을 직접 확인했다.",
    minutes: 4,
    effect: { mental: -2, boss: -1 },
  },
];

const state = {
  scene: "home",
  scheduleIndex: 0,
  absoluteMinutes: 20 * 60 + 40,
  dayIndex: 0,
  minuteOfDay: 20 * 60 + 40,
  target: null,
  phoneOpen: false,
  phoneView: "home",
  phoneMarket: "domestic",
  commuteOpen: false,
  scheduleOpen: false,
  shopOpen: false,
  missedShifts: 0,
  fired: false,
  activeShift: null,
  monthDay: 8,
  rentDue: RENT_TOTAL,
  roomChecks: 1,
  stockProfit: 8400,
  stockTick: 0,
  bankHistory: [
    { type: "입금", title: "지난 알바 급여", amount: 61920 },
    { type: "지출", title: "교통카드 충전", amount: -10000 },
    { type: "저축", title: "자취자금 이동", amount: -30000 },
  ],
  player: { x: 690, y: 355, speed: 230 },
  stats: {
    savings: 301000,
    cash: 61920,
    mental: 49,
    stamina: 57,
    boss: 81,
    customer: 66,
  },
  progress: {},
  lastShift: {
    hours: "-",
    pay: 0,
    boss: 0,
    customer: 0,
    success: 0,
  },
  logs: [
    "[알림] 현관문 앞에서 E를 누르면 스케줄에 맞는 알바 장소로 출근해.",
    "[규칙] 업무를 70% 이상 처리해야 퇴근 정산이 가능해.",
    "[생활] 식사는 아이템 구매에서 산 음식이나 음료가 있어야 할 수 있어.",
  ],
  inventory: [],
};

syncClock();
updateStockProfit();

const scenes = {
  home: {
    badge: "내 자취방",
    sideTitle: "퇴근 후 할 일",
    start: { x: 690, y: 355 },
    bounds: { minX: 40, maxX: 1290, minY: 70, maxY: 650 },
    blockers: [],
    tasks: [
      createTask({
        id: "sleep",
        name: "잠자기",
        icon: "Z",
        x: 365,
        y: 438,
        target: 1,
        minutes: 360,
        effect: { mental: 10, stamina: 30 },
        message: "푹 잤다. 다음 알바를 버틸 체력이 돌아왔다.",
      }),
      createTask({
        id: "meal",
        name: "식사 하기",
        icon: "🍽️",
        x: 835,
        y: 190,
        target: 1,
        minutes: 20,
        effect: {},
        message: "구매해 둔 음식으로 식사를 했다.",
        action: "eatMeal",
      }),
      createTask({
        id: "laundry",
        name: "빨래하기",
        icon: "🧺",
        x: 1075,
        y: 462,
        target: 1,
        minutes: 45,
        effect: { stamina: -5, boss: 1 },
        message: "근무복 빨래 완료. 다음 출근 때는 찝찝함이 덜하다.",
      }),
      createTask({
        id: "schedule",
        name: "스케줄 확인",
        icon: "📅",
        x: 520,
        y: 175,
        target: 1,
        minutes: 10,
        effect: { mental: 2 },
        message: "이번 주 근무표를 확인했다. 지각 리스크가 줄었다.",
      }),
      createTask({
        id: "rent",
        name: "월세 확인",
        icon: "🏠",
        x: 1160,
        y: 535,
        target: 1,
        minutes: 10,
        effect: { mental: -1 },
        message: "월세와 관리비 납부 일정을 확인했다.",
        action: "rent",
      }),
      createTask({
        id: "roomCheck",
        name: "방 체크",
        icon: "✓",
        x: 845,
        y: 545,
        target: 1,
        minutes: 35,
        effect: { mental: 2, stamina: -2 },
        message: "방 구하기 체크리스트를 하나 더 채웠다.",
        action: "roomCheck",
      }),
      createTask({
        id: "buyItems",
        name: "아이템 구매",
        icon: "🛒",
        x: 1195,
        y: 430,
        target: 1,
        minutes: 15,
        effect: { mental: -1 },
        message: "다음 근무를 버틸 간식과 물을 샀다.",
        action: "buyItems",
      }),
    ],
    memory: [],
  },
  store: {
    badge: "편의점",
    sideTitle: "오늘의 업무",
    start: { x: 575, y: 250 },
    bounds: { minX: 35, maxX: 1290, minY: 72, maxY: 650 },
    blockers: [],
    tasks: [
      createTask({
        id: "storeCheckout",
        name: "계산 5회",
        icon: "💳",
        x: 355,
        y: 220,
        target: 5,
        minutes: 8,
        effect: { customer: 1, stamina: -2 },
        message: "바코드와 결제를 빠르게 처리했다.",
      }),
      createTask({
        id: "storeTobacco",
        name: "담배 찾기 2회",
        icon: "▦",
        x: 510,
        y: 130,
        target: 2,
        minutes: 7,
        effect: { boss: 1, mental: -1 },
        message: "손님이 말한 담배 위치를 기억해서 찾아냈다.",
      }),
      createTask({
        id: "storeExpired",
        name: "폐기 확인 3회",
        icon: "⏱",
        x: 1190,
        y: 425,
        target: 3,
        minutes: 10,
        effect: { boss: 1, stamina: -1 },
        message: "폐기 시간을 확인하고 기록했다.",
      }),
      createTask({
        id: "storeCleaning",
        name: "청소 2회",
        icon: "🧹",
        x: 1110,
        y: 205,
        target: 2,
        minutes: 12,
        effect: { boss: 1, stamina: -3 },
        message: "전자레인지와 바닥 주변을 정리했다.",
      }),
      createTask({
        id: "storeRamenShelf",
        name: "라면 진열 2회",
        icon: "🍜",
        x: 430,
        y: 392,
        target: 2,
        minutes: 9,
        effect: { boss: 1, stamina: -2 },
        message: "창고에서 라면 박스를 꺼내 매대를 채웠다.",
      }),
      createTask({
        id: "storeSnackShelf",
        name: "과자 진열 2회",
        icon: "🍪",
        x: 735,
        y: 392,
        target: 2,
        minutes: 9,
        effect: { boss: 1, stamina: -2 },
        message: "과자 코너 빈 칸을 채웠다.",
      }),
      createTask({
        id: "storeParcel",
        name: "택배 찾기",
        icon: "📦",
        x: 130,
        y: 270,
        target: 1,
        minutes: 8,
        effect: { customer: 1, mental: -1 },
        message: "보관함 번호를 확인하고 택배를 찾아줬다.",
      }),
    ],
    memory: [
      ["▦", "담배 위치", "암기 중"],
      ["🍜", "라면 창고 위치", "암기 중"],
      ["⏱", "폐기 시간", "암기 중"],
      ["📦", "택배 보관함", "암기 중"],
    ],
  },
  cafe: {
    badge: "카페",
    sideTitle: "오늘의 업무",
    start: { x: 635, y: 405 },
    bounds: { minX: 35, maxX: 1290, minY: 72, maxY: 650 },
    blockers: [],
    tasks: [
      createTask({
        id: "cafeOrder",
        name: "주문 받기 4회",
        icon: "🧾",
        x: 545,
        y: 370,
        target: 4,
        minutes: 7,
        effect: { customer: 1, stamina: -1 },
        message: "손님 주문과 옵션을 확인했다.",
      }),
      createTask({
        id: "cafeAmericano",
        name: "아메리카노 제조",
        icon: "☕",
        x: 565,
        y: 205,
        target: 3,
        minutes: 9,
        effect: { boss: 1, stamina: -2 },
        message: "샷 추출, 물, 얼음 순서를 맞춰 음료를 만들었다.",
      }),
      createTask({
        id: "cafeDessert",
        name: "디저트 포장",
        icon: "🍰",
        x: 355,
        y: 385,
        target: 2,
        minutes: 8,
        effect: { customer: 1, mental: -1 },
        message: "디저트와 포크 수량을 확인해서 포장했다.",
      }),
      createTask({
        id: "cafeTable",
        name: "테이블 정리",
        icon: "🧽",
        x: 255,
        y: 555,
        target: 3,
        minutes: 8,
        effect: { boss: 1, stamina: -2 },
        message: "컵과 쟁반을 치우고 테이블을 닦았다.",
      }),
      createTask({
        id: "cafeDishes",
        name: "설거지",
        icon: "🧼",
        x: 850,
        y: 230,
        target: 2,
        minutes: 12,
        effect: { boss: 1, stamina: -3 },
        message: "피처와 컵을 씻어 다시 쓸 수 있게 했다.",
      }),
      createTask({
        id: "cafeStock",
        name: "원두 보충",
        icon: "☕",
        x: 1135,
        y: 355,
        target: 1,
        minutes: 10,
        effect: { boss: 1, stamina: -2 },
        message: "원두와 컵 재고를 확인해 부족한 것을 채웠다.",
      }),
    ],
    memory: [
      ["☕", "아메리카노 순서", "샷-물-얼음"],
      ["컵", "컵 사이즈", "암기 중"],
      ["🍰", "포장 세트", "암기 중"],
      ["원두", "재고 위치", "암기 중"],
    ],
  },
  pcbang: {
    badge: "PC방",
    sideTitle: "오늘의 업무",
    start: { x: 620, y: 250 },
    bounds: { minX: 35, maxX: 1290, minY: 72, maxY: 650 },
    blockers: [],
    tasks: [
      createTask({
        id: "pcCheckout",
        name: "카운터 계산",
        icon: "💳",
        x: 465,
        y: 165,
        target: 4,
        minutes: 7,
        effect: { customer: 1, stamina: -1 },
        message: "시간 충전과 간식 결제를 처리했다.",
      }),
      createTask({
        id: "pcSeat",
        name: "좌석 정리",
        icon: "⌨",
        x: 390,
        y: 390,
        target: 4,
        minutes: 8,
        effect: { boss: 1, stamina: -2 },
        message: "키보드, 마우스, 헤드셋을 제자리로 정리했다.",
      }),
      createTask({
        id: "pcRamen",
        name: "라면 조리",
        icon: "🍜",
        x: 1040,
        y: 215,
        target: 3,
        minutes: 9,
        effect: { customer: 1, stamina: -2 },
        message: "주문표대로 라면과 토핑을 준비했다.",
      }),
      createTask({
        id: "pcDrink",
        name: "음료 보충",
        icon: "🥤",
        x: 860,
        y: 150,
        target: 2,
        minutes: 8,
        effect: { boss: 1, stamina: -2 },
        message: "냉장고 빈 칸에 음료를 채웠다.",
      }),
      createTask({
        id: "pcPrint",
        name: "프린터 도움",
        icon: "🖨",
        x: 130,
        y: 255,
        target: 1,
        minutes: 8,
        effect: { customer: 1, mental: -1 },
        message: "출력 설정을 도와주고 용지를 채웠다.",
      }),
      createTask({
        id: "pcTrash",
        name: "분리수거",
        icon: "♻",
        x: 1050,
        y: 445,
        target: 2,
        minutes: 10,
        effect: { boss: 1, stamina: -3 },
        message: "캔, 플라스틱, 일반 쓰레기를 나눠 버렸다.",
      }),
    ],
    memory: [
      ["⌨", "좌석 번호", "암기 중"],
      ["🍜", "조리 시간", "암기 중"],
      ["🖨", "프린터 위치", "암기 중"],
      ["♻", "분리수거 규칙", "암기 중"],
    ],
  },
  restaurant: {
    badge: "음식점",
    sideTitle: "오늘의 업무",
    start: { x: 665, y: 375 },
    bounds: { minX: 35, maxX: 1290, minY: 72, maxY: 650 },
    blockers: [],
    tasks: [
      createTask({
        id: "foodOrder",
        name: "주문 받기",
        icon: "🧾",
        x: 300,
        y: 470,
        target: 4,
        minutes: 8,
        effect: { customer: 1, stamina: -1 },
        message: "테이블 번호와 메뉴를 확인했다.",
      }),
      createTask({
        id: "foodServe",
        name: "음식 세팅",
        icon: "🍽",
        x: 620,
        y: 330,
        target: 4,
        minutes: 9,
        effect: { customer: 1, stamina: -2 },
        message: "나온 음식을 테이블에 맞게 가져다줬다.",
      }),
      createTask({
        id: "foodSide",
        name: "반찬 리필",
        icon: "🥢",
        x: 820,
        y: 255,
        target: 3,
        minutes: 7,
        effect: { boss: 1, stamina: -2 },
        message: "비어 있는 반찬통을 확인하고 리필했다.",
      }),
      createTask({
        id: "foodDishes",
        name: "그릇 치우기",
        icon: "🧼",
        x: 1105,
        y: 365,
        target: 3,
        minutes: 10,
        effect: { boss: 1, stamina: -3 },
        message: "빈 그릇을 회수하고 퇴식구로 옮겼다.",
      }),
      createTask({
        id: "foodPay",
        name: "계산",
        icon: "💳",
        x: 610,
        y: 205,
        target: 3,
        minutes: 7,
        effect: { customer: 1, mental: -1 },
        message: "테이블 금액과 결제를 확인했다.",
      }),
      createTask({
        id: "foodClean",
        name: "바닥 청소",
        icon: "🧹",
        x: 1050,
        y: 550,
        target: 2,
        minutes: 12,
        effect: { boss: 1, stamina: -3 },
        message: "미끄러운 바닥과 쓰레기를 정리했다.",
      }),
    ],
    memory: [
      ["T", "테이블 번호", "암기 중"],
      ["🥢", "기본 반찬 구성", "암기 중"],
      ["🍽", "주방 호출 위치", "암기 중"],
      ["💳", "영수증 처리", "암기 중"],
    ],
  },
};

function image(src) {
  const img = new Image();
  img.src = src;
  img.addEventListener("load", draw);
  return img;
}

function rect(x, y, w, h) {
  return { x, y, w, h };
}

function createTask(config) {
  return {
    kind: "task",
    action: "",
    target: 1,
    minutes: 10,
    effect: {},
    ...config,
  };
}

function money(value) {
  return `${Math.round(value).toLocaleString("ko-KR")}원`;
}

function signedMoney(value) {
  const prefix = value > 0 ? "+" : "";
  return `${prefix}${money(value)}`;
}

function signedPct(value) {
  const prefix = value > 0 ? "+" : "";
  return `${prefix}${value.toFixed(1)}%`;
}

function timeText(minutes) {
  const safe = ((minutes % 1440) + 1440) % 1440;
  return `${String(Math.floor(safe / 60)).padStart(2, "0")}:${String(safe % 60).padStart(2, "0")}`;
}

function portfolioValue() {
  return stocks.reduce((sum, stock) => sum + stock.owned * stock.price, 0);
}

function investmentCost() {
  return stocks.reduce((sum, stock) => sum + stock.owned * stock.avg, 0);
}

function currentStockProfit() {
  return Math.round(portfolioValue() - investmentCost());
}

function accountTotal() {
  return Math.round(state.stats.cash + state.stats.savings + portfolioValue());
}

function updateStockProfit() {
  state.stockProfit = currentStockProfit();
}

function updateStockPrices(minutes) {
  const steps = Math.max(1, Math.ceil(minutes / 30));
  state.stockTick += steps;
  for (const stock of stocks) {
    const previous = stock.price;
    const drift = Math.sin((state.stockTick + stock.basePrice / 1000) * 0.7) * stock.volatility * 0.45;
    const noise = (Math.random() - 0.5) * stock.volatility;
    stock.price = Math.max(1000, Math.round(previous * (1 + drift + noise)));
    stock.change = ((stock.price - previous) / previous) * 100;
  }
  updateStockProfit();
}

function stockById(id) {
  return stocks.find((stock) => stock.id === id);
}

function addBankHistory(type, title, amount) {
  state.bankHistory.unshift({ type, title, amount });
  state.bankHistory = state.bankHistory.slice(0, 6);
}

function syncClock() {
  state.dayIndex = Math.floor(state.absoluteMinutes / 1440) % 7;
  state.minuteOfDay = state.absoluteMinutes % 1440;
  state.monthDay = 8 + Math.floor(state.absoluteMinutes / 1440);
}

function clamp(value, min, max) {
  return Math.max(min, Math.min(max, value));
}

function currentScene() {
  return scenes[state.scene];
}

function currentShift() {
  return weeklySchedule[state.scheduleIndex];
}

function isWorkScene() {
  return state.scene !== "home";
}

function shiftText(shift = currentShift()) {
  if (shift.period === "휴무") return "휴무";
  return `${shift.period} ${shift.place}`;
}

function clockText() {
  return `${dayNames[state.dayIndex]}요일 ${timeText(state.minuteOfDay)}`;
}

function pointInRect(point, area) {
  return point.x >= area.x && point.x <= area.x + area.w && point.y >= area.y && point.y <= area.y + area.h;
}

function progressOf(taskItem) {
  return state.progress[taskItem.id] || 0;
}

function setProgress(taskItem, value) {
  state.progress[taskItem.id] = clamp(value, 0, taskItem.target);
}

function isDone(taskItem) {
  return progressOf(taskItem) >= taskItem.target;
}

function addLog(message) {
  state.logs.unshift(message);
  state.logs = state.logs.slice(0, 5);
  renderUi();
}

function showToast(message) {
  toast.textContent = message;
  toast.classList.add("is-visible");
  clearTimeout(showToast.timer);
  showToast.timer = setTimeout(() => toast.classList.remove("is-visible"), 1600);
}

function applyEffect(effect) {
  for (const [key, value] of Object.entries(effect)) {
    if (key === "cash" || key === "savings") {
      state.stats[key] = Math.max(0, state.stats[key] + value);
    } else {
      state.stats[key] = clamp(state.stats[key] + value, 0, 100);
    }
  }
}

function advanceMinutes(minutes) {
  state.absoluteMinutes += minutes;
  syncClock();
  updateStockPrices(minutes);
  checkMissedCurrentShift();
}

function shiftAbsStart(shift) {
  const weekStart = Math.floor(state.absoluteMinutes / WEEK_MINUTES) * WEEK_MINUTES;
  let start = weekStart + shift.dayIndex * 1440 + shift.start;
  if (start < state.absoluteMinutes - 12 * 60) start += WEEK_MINUTES;
  return start;
}

function checkMissedCurrentShift() {
  if (state.fired || state.activeShift || state.scene !== "home" || state.commuteOpen) return false;

  const shift = currentShift();
  if (!shift || shift.duration <= 0 || shift.period === "휴무") return false;

  const startAbs = shiftAbsStart(shift);
  const endAbs = startAbs + shift.duration;
  if (state.absoluteMinutes <= endAbs) return false;

  recordMissedShift(shift);
  return true;
}

function recordMissedShift(shift) {
  const missedText = `${dayNames[shift.dayIndex]}요일 ${timeText(shift.start)} ${shiftText(shift)}`;
  state.missedShifts += 1;
  state.activeShift = null;
  state.progress = {};
  state.stats.boss = clamp(state.stats.boss - 18, 0, 100);
  state.stats.customer = clamp(state.stats.customer - 4, 0, 100);
  state.stats.mental = clamp(state.stats.mental - 8, 0, 100);
  state.lastShift = {
    hours: "결근",
    pay: 0,
    boss: -18,
    customer: -4,
    success: 0,
  };
  state.scheduleIndex = (state.scheduleIndex + 1) % weeklySchedule.length;

  if (state.missedShifts >= 3) {
    state.fired = true;
    addLog(`[해고] ${missedText} 근무까지 출근 실패 3회가 누적되어 알바에서 잘렸다.`);
    showToast("출근 실패 3회로 해고됐다.");
    return;
  }

  addLog(`[경고] ${missedText} 근무에 출근하지 못했다. 결근 경고 ${state.missedShifts}/3회.`);
  showToast(`출근 실패 경고 ${state.missedShifts}/3`);
}

function dist(a, b) {
  return Math.hypot(a.x - b.x, a.y - b.y);
}

function interactiveTasks() {
  if (isWorkScene()) return [...currentScene().tasks, clockOutTask];
  return currentScene().tasks;
}

function nearestTask() {
  let found = null;
  for (const taskItem of interactiveTasks()) {
    const d = dist(state.player, taskItem);
    if (d < 115 && (!found || d < found.d)) found = { ...taskItem, d };
  }
  return found;
}

function pointedTask(point) {
  let found = null;
  for (const taskItem of interactiveTasks()) {
    const d = dist(point, taskItem);
    if (d < 96 && (!found || d < found.d)) found = { ...taskItem, d };
  }
  return found;
}

function playerNearDoor() {
  return state.scene === "home" && pointInRect(state.player, doorInteractArea);
}

function playerRectAt(x, y) {
  return { x: x - 21, y: y - 12, w: 42, h: 26 };
}

function rectsOverlap(a, b) {
  return a.x < b.x + b.w && a.x + a.w > b.x && a.y < b.y + b.h && a.y + a.h > b.y;
}

function isBlocked(x, y) {
  const scene = currentScene();
  if (x < scene.bounds.minX || x > scene.bounds.maxX || y < scene.bounds.minY || y > scene.bounds.maxY) return true;
  const foot = playerRectAt(x, y);
  return scene.blockers.some((blocker) => rectsOverlap(foot, blocker));
}

function movePlayer(dx, dy, amount) {
  const length = Math.hypot(dx, dy);
  if (!length) return;
  const nx = dx / length;
  const ny = dy / length;
  const nextX = state.player.x + nx * amount;
  const nextY = state.player.y + ny * amount;
  if (!isBlocked(nextX, state.player.y)) state.player.x = nextX;
  if (!isBlocked(state.player.x, nextY)) state.player.y = nextY;
}

function workTasks() {
  if (!isWorkScene()) return [];
  return currentScene().tasks;
}

function workTotals() {
  const tasks = workTasks();
  const total = tasks.reduce((sum, taskItem) => sum + taskItem.target, 0);
  const done = tasks.reduce((sum, taskItem) => sum + Math.min(progressOf(taskItem), taskItem.target), 0);
  return { total, done, pct: total ? Math.round((done / total) * 100) : 0 };
}

function isRestShift(shift = currentShift()) {
  return shift.period === "휴무";
}

function homePrepTasksDone() {
  return scenes.home.tasks.filter((taskItem) => taskItem.id !== "sleep").every(isDone);
}

function finishRestDayBySleeping(taskItem) {
  applyEffect(taskItem.effect);

  if (isRestShift() && homePrepTasksDone()) {
    const restShift = currentShift();
    const nextIndex = (state.scheduleIndex + 1) % weeklySchedule.length;
    const restStart = shiftAbsStart(restShift);
    const nextMorning = restStart + 1440 + 8 * 60;
    const minutes = Math.max(60, nextMorning - state.absoluteMinutes);

    advanceMinutes(minutes);
    state.scheduleIndex = nextIndex;
    state.progress = {};
    addLog("[휴무] 할 일을 다 끝내고 푹 잤다. 다음날 아침으로 넘어갔다.");
    showToast("다음날 아침!");
    renderUi();
    return;
  }

  advanceMinutes(taskItem.minutes);
  if (!isRestShift()) setProgress(taskItem, 1);
  addLog(isRestShift() ? "[휴무] 아직 할 일이 남아 있어서 잠깐만 쉬었다." : `[완료] ${taskItem.message}`);
  showToast(isRestShift() ? "할 일 끝내고 자면 다음날로 넘어가." : "잠자기 완료!");
  renderUi();
}

function addInventory(name, icon, count) {
  const item = state.inventory.find((entry) => entry.name === name);
  if (item) item.count += count;
  else state.inventory.push({ name, icon, count });
}

function useInventory(name) {
  const item = state.inventory.find((entry) => entry.name === name && entry.count > 0);
  if (!item) return false;
  item.count -= 1;
  state.inventory = state.inventory.filter((entry) => entry.count > 0);
  return true;
}

function useMealInventory() {
  const item = state.inventory.find((entry) => mealItemEffects[entry.name] && entry.count > 0);
  if (!item) return null;
  item.count -= 1;
  state.inventory = state.inventory.filter((entry) => entry.count > 0);
  return item;
}

function completeMealTask(taskItem) {
  const item = useMealInventory();
  if (!item) {
    showToast("먹을 아이템이 없어.");
    addLog("[식사] 먹을 수 있는 구매 아이템이 없다. 아이템 구매에서 식사거리를 먼저 사야 한다.");
    return;
  }

  const meal = mealItemEffects[item.name];
  setProgress(taskItem, progressOf(taskItem) + 1);
  advanceMinutes(taskItem.minutes);
  applyEffect(meal.effect);
  addLog(`[식사] ${item.icon} ${meal.message}`);
  showToast(`${item.name} 먹었다!`);
  renderUi();
}

function handleHomeAction(taskItem) {
  if (taskItem.action === "save") {
    const amount = Math.min(30000, state.stats.cash);
    if (amount <= 0) {
      addLog("[생활] 지금은 저축할 현금이 없다.");
      return;
    }
    state.stats.cash -= amount;
    state.stats.savings += amount;
    addLog(`[저축] ${money(amount)}을 자취자금 통장에 넣었다.`);
    return;
  }

  if (taskItem.action === "rent") {
    if (state.rentDue <= 0) {
      addLog("[월세] 이번 달 월세와 관리비는 이미 냈다.");
      return;
    }

    if (state.stats.cash >= state.rentDue) {
      state.stats.cash -= state.rentDue;
      addLog(`[월세] 월세와 관리비 ${money(state.rentDue)}을 납부했다.`);
      state.rentDue = 0;
    } else {
      addLog(`[월세] 이번 달 예상 납부액은 ${money(state.rentDue)}. 아직 현금이 부족하다.`);
    }
    return;
  }

  if (taskItem.action === "roomCheck") {
    state.roomChecks = clamp(state.roomChecks + 1, 0, 5);
    addLog(`[방 구하기] 체크리스트 ${state.roomChecks}/5 완료. 채광, 방음, 관리비를 더 봐야 한다.`);
    return;
  }

  if (taskItem.action === "buyItems") {
    showShopPanel();
  }
}

function transferToSavings(amount) {
  const actual = Math.min(amount, state.stats.cash);
  if (actual <= 0) {
    showToast("저축할 현금이 없어.");
    addLog("[토스] 지금은 자취자금으로 옮길 현금이 없다.");
    return;
  }

  state.stats.cash -= actual;
  state.stats.savings += actual;
  addBankHistory("저축", "자취자금 저축", -actual);
  advanceMinutes(2);
  addLog(`[토스] ${money(actual)}을 자취자금 통장에 넣었다.`);
  showToast(`${money(actual)} 저축 완료!`);
  renderUi();
  if (state.phoneOpen) renderPhone();
}

function buyShopItem(id) {
  const item = shopItems.find((entry) => entry.id === id);
  if (!item) return;

  if (state.stats.cash < item.price) {
    showToast(`${item.name} 살 돈이 부족해.`);
    addLog(`[구매] ${item.name}을 사려면 ${money(item.price)}이 필요하다.`);
    return;
  }

  state.stats.cash -= item.price;
  addInventory(item.name, item.icon, 1);
  addBankHistory("지출", item.name, -item.price);
  advanceMinutes(3);

  const buyTask = scenes.home.tasks.find((taskItem) => taskItem.id === "buyItems");
  if (buyTask && !isDone(buyTask)) {
    setProgress(buyTask, 1);
    applyEffect(buyTask.effect);
  }

  addLog(`[구매] ${item.icon} ${item.name}을 샀다. (-${money(item.price)})`);
  showToast(`${item.name} 구매 완료!`);
  renderUi();
  renderShopPanel();
}

function buyStock(id, mode = "unit") {
  const stock = stockById(id);
  if (!stock) return;
  const amount = Math.floor(mode === "allin" ? state.stats.cash : Math.min(STOCK_TRADE_AMOUNT, state.stats.cash));
  if (amount < 1000) {
    showToast("매수할 현금이 부족해.");
    addLog("[주식] 매수할 현금이 부족하다.");
    return;
  }

  const quantity = amount / stock.price;
  const previousCost = stock.owned * stock.avg;
  stock.owned += quantity;
  stock.avg = (previousCost + amount) / stock.owned;
  state.stats.cash -= amount;
  addBankHistory("투자", `${stock.name} 매수`, -amount);
  advanceMinutes(5);
  updateStockProfit();
  addLog(`[주식] ${stock.name} ${money(amount)} ${mode === "allin" ? "올인 매수" : "매수"}. 평가손익 ${signedMoney(state.stockProfit)}.`);
  showToast(`${stock.name} ${mode === "allin" ? "올인" : "매수"} 완료`);
  renderUi();
  if (state.phoneOpen) renderPhone();
}

function sellStock(id) {
  const stock = stockById(id);
  if (!stock || stock.owned <= 0) {
    showToast("팔 보유 수량이 없어.");
    return;
  }

  const quantity = stock.owned;
  const amount = quantity * stock.price;
  const realizedProfit = amount - quantity * stock.avg;
  stock.owned = 0;
  stock.avg = 0;

  state.stats.cash += Math.round(amount);
  addBankHistory("매도", `${stock.name} 매도`, Math.round(amount));
  advanceMinutes(5);
  updateStockProfit();
  addLog(`[주식] ${stock.name} ${money(amount)} 매도. 실현손익 ${signedMoney(realizedProfit)}.`);
  showToast(`${stock.name} 매도 완료`);
  renderUi();
  if (state.phoneOpen) renderPhone();
}

function nextWorkingShift() {
  for (let i = 1; i <= weeklySchedule.length; i += 1) {
    const shift = weeklySchedule[(state.scheduleIndex + i) % weeklySchedule.length];
    if (shift.period !== "휴무") return shift;
  }
  return currentShift();
}

function offdaySuggestions() {
  const next = nextWorkingShift();
  return [
    `다음 근무: ${dayNames[next.dayIndex]}요일 ${timeText(next.start)} ${shiftText(next)}`,
    "잠자기로 체력 회복하기",
    "빨래해서 근무복 준비하기",
    "월세/관리비 납부 가능 여부 확인하기",
    "라면/생수 같은 비상 아이템 채워두기",
    "주식앱에서 평가손익 보고 무리한 매수 참기",
    "할 일을 다 끝낸 뒤 잠자기를 누르면 다음날 아침으로 이동",
  ];
}

function maybeWorkEvent() {
  if (!state.activeShift || Math.random() > 0.28) return;
  const pool = randomEvents.filter((event) => event.jobs.includes(state.scene));
  const event = pool[Math.floor(Math.random() * pool.length)];
  advanceMinutes(event.minutes);
  applyEffect(event.effect);
  state.activeShift.events += 1;
  addLog(`[변수] ${event.title}: ${event.message}`);
}

function interact() {
  if (state.commuteOpen || state.scheduleOpen || state.shopOpen) return;

  if (playerNearDoor()) {
    showCommuteChoice();
    return;
  }

  const taskItem = nearestTask();
  if (!taskItem) {
    showToast("상호작용할 곳 근처로 이동해야 해.");
    return;
  }

  if (taskItem.kind === "clockOut") {
    finishShift();
    return;
  }

  if (state.scene === "home" && taskItem.id === "schedule") {
    if (!isDone(taskItem)) {
      setProgress(taskItem, progressOf(taskItem) + 1);
      advanceMinutes(taskItem.minutes);
      applyEffect(taskItem.effect);
      addLog(`[완료] ${taskItem.message}`);
      showToast("시간표 확인 완료!");
    }
    showSchedulePanel();
    renderUi();
    return;
  }

  if (state.scene === "home" && taskItem.id === "buyItems") {
    showShopPanel();
    return;
  }

  if (isDone(taskItem)) {
    showToast("이미 처리한 일이야.");
    return;
  }

  if (state.scene === "home" && taskItem.id === "meal") {
    completeMealTask(taskItem);
    return;
  }

  if (state.scene === "home" && taskItem.id === "sleep") {
    finishRestDayBySleeping(taskItem);
    return;
  }

  setProgress(taskItem, progressOf(taskItem) + 1);
  advanceMinutes(taskItem.minutes);
  applyEffect(taskItem.effect);

  if (state.scene === "home") {
    handleHomeAction(taskItem);
  } else {
    maybeWorkEvent();
  }

  const done = isDone(taskItem);
  showToast(done ? `${taskItem.name} 완료!` : `${taskItem.name} ${progressOf(taskItem)} / ${taskItem.target}`);
  addLog(`[완료] ${taskItem.message}`);

  const totals = workTotals();
  if (isWorkScene() && state.activeShift && totals.pct >= WORK_PASS_RATE && !state.activeShift.passLogged) {
    state.activeShift.passLogged = true;
    addLog(`[퇴근 가능] 업무 ${totals.pct}% 완료. 입구 쪽에서 퇴근 정산을 처리할 수 있어.`);
  }

  renderUi();
}

function showCommuteChoice() {
  const shift = currentShift();
  if (state.scene !== "home") return;

  if (state.fired) {
    showToast("이미 해고돼서 출근할 수 없어.");
    addLog("[해고] 이 알바는 더 이상 출근할 수 없다. 새 알바를 구해야 한다.");
    return;
  }

  if (checkMissedCurrentShift()) {
    renderUi();
    return;
  }

  if (shift.period === "휴무") {
    showToast("오늘은 휴무야. 집에서 회복하자.");
    addLog("[스케줄] 오늘은 휴무. 자취방 정리와 휴식에 집중하자.");
    return;
  }

  togglePhone(false);
  state.commuteOpen = true;
  renderCommutePanel(shift);
  commutePanel.classList.add("is-visible");
  commutePanel.setAttribute("aria-hidden", "false");
}

function closeCommuteChoice() {
  state.commuteOpen = false;
  commutePanel.classList.remove("is-visible");
  commutePanel.setAttribute("aria-hidden", "true");
  focusGame();
}

function renderCommutePanel(shift) {
  const startAbs = shiftAbsStart(shift);
  const lateInfo = Object.entries(commuteOptions)
    .map(([key, option]) => {
      const arriveAbs = state.absoluteMinutes + option.minutes;
      const late = Math.max(0, arriveAbs - startAbs);
      return { key, option, late };
    });

  commutePanel.innerHTML = `
    <div class="commute-card" role="dialog" aria-modal="true" aria-label="출근 이동 선택">
      <button class="commute-close" type="button" aria-label="닫기">×</button>
      <h2>출근 이동 선택</h2>
      <p class="commute-sub">${dayNames[shift.dayIndex]}요일 ${timeText(shift.start)} ${shiftText(shift)} 근무</p>
      <div class="commute-options">
        ${lateInfo
          .map(
            ({ key, option, late }) => `
              <button class="commute-option" type="button" data-commute="${key}">
                <strong>${option.icon} ${option.label}</strong>
                <span>${option.minutes}분 · ${money(option.cost)}</span>
                <small>${late > 0 ? `${late}분 지각 위험` : "정시 도착 가능"}</small>
                <em>${option.note}</em>
              </button>
            `,
          )
          .join("")}
      </div>
    </div>
  `;
}

function startCommute(method) {
  const option = commuteOptions[method];
  const shift = currentShift();
  if (state.fired) {
    showToast("이미 해고돼서 출근할 수 없어.");
    return;
  }
  if (!option || shift.period === "휴무") return;

  if (state.stats.cash < option.cost) {
    showToast(`${option.label} 비용이 부족해.`);
    addLog(`[출근] ${option.label} 비용 ${money(option.cost)}이 부족하다.`);
    return;
  }

  const startAbs = shiftAbsStart(shift);
  const arriveAbs = state.absoluteMinutes + option.minutes;
  const lateMinutes = Math.max(0, arriveAbs - startAbs);
  const actualStartAbs = lateMinutes > 0 ? arriveAbs : startAbs;

  state.stats.cash -= option.cost;
  applyEffect(option.effect);
  state.absoluteMinutes = actualStartAbs;
  syncClock();
  state.progress = {};
  state.activeShift = {
    ...shift,
    startAbs,
    endAbs: startAbs + shift.duration,
    commute: option.label,
    lateMinutes,
    events: 0,
    passLogged: false,
  };

  if (lateMinutes > 0) {
    const bossPenalty = Math.min(5, Math.ceil(lateMinutes / 10));
    applyEffect({ boss: -bossPenalty, mental: -2 });
    addLog(`[출근] ${option.label}로 이동했지만 ${lateMinutes}분 지각했다. 사장님 평점이 내려갔다.`);
  } else {
    addLog(`[출근] ${option.label}로 이동했다. ${dayNames[shift.dayIndex]}요일 ${timeText(shift.start)} ${shiftText(shift)} 근무 시작.`);
  }

  closeCommuteChoice();
  setScene(shift.scene);
  showToast(`${shiftText(shift)} 근무 시작!`);
}

function finishShift() {
  if (!isWorkScene() || !state.activeShift) return;
  const totals = workTotals();
  if (totals.pct < WORK_PASS_RATE) {
    showToast(`업무 ${WORK_PASS_RATE}% 이상 필요해. 현재 ${totals.pct}%`);
    addLog(`[주의] 아직 퇴근하기 이르다. 현재 업무 처리율은 ${totals.pct}%야.`);
    return;
  }

  const shift = state.activeShift;
  const payRate = totals.pct >= 100 ? 1 : 0.94;
  const pay = Math.round(shift.pay * payRate);
  const bossDelta = clamp(Math.round((totals.pct - 70) / 10) + 1 - shift.events - Math.ceil(shift.lateMinutes / 15), -7, 7);
  const customerDelta = clamp(Math.round((totals.pct - 70) / 12) + 1, -4, 6);
  const remaining = Math.max(0, shift.endAbs - state.absoluteMinutes);

  advanceMinutes(remaining);
  state.stats.cash += pay;
  state.stats.boss = clamp(state.stats.boss + bossDelta, 0, 100);
  state.stats.customer = clamp(state.stats.customer + customerDelta, 0, 100);
  state.stats.stamina = clamp(state.stats.stamina - (shift.period === "야간" ? 14 : 9), 0, 100);
  state.stats.mental = clamp(state.stats.mental + (totals.pct >= 85 ? 3 : -4), 0, 100);
  state.lastShift = {
    hours: `${Math.floor(shift.duration / 60)}시간 ${shift.duration % 60}분`,
    pay,
    boss: bossDelta,
    customer: customerDelta,
    success: totals.pct,
  };

  state.activeShift = null;
  state.scheduleIndex = (state.scheduleIndex + 1) % weeklySchedule.length;
  state.progress = {};
  setScene("home");
  addLog(`[퇴근] ${state.lastShift.hours} 근무 완료. 급여 ${money(pay)}이 현금으로 들어왔다.`);
  if (state.stats.boss <= 0 || state.stats.customer <= 0) {
    addLog("[경고] 평점이 0에 가까워 해고 위험이 있다. 다음 근무에서 회복해야 해.");
  }
}

function setScene(sceneName) {
  if (!scenes[sceneName]) return;
  state.scene = sceneName;
  state.target = null;
  state.player = { ...state.player, ...scenes[sceneName].start };
  renderUi();
  draw();
  focusGame();
}

function renderUi() {
  const scene = currentScene();
  const shift = currentShift();
  const totals = workTotals();
  const currentWork = state.activeShift || shift;

  sceneBadge.textContent = state.scene === "home" ? scene.badge : `${shiftText(currentWork)} 근무 중`;
  sideTitle.textContent = state.scene === "home" ? scene.sideTitle : scene.sideTitle;
  dayTimeText.textContent = clockText();
  shiftPlaceText.textContent =
    state.fired
      ? "해고 상태"
      : state.scene === "home"
        ? `다음: ${shiftText(shift)}`
        : `${shiftText(currentWork)} 진행 ${totals.pct}%`;
  nextWorkText.textContent =
    state.fired
      ? `출근 실패 ${state.missedShifts}/3회 · 새 알바 필요`
      : state.scene === "home"
      ? shift.period === "휴무"
        ? `${dayNames[shift.dayIndex]}요일 휴무`
        : `${dayNames[shift.dayIndex]}요일 ${timeText(shift.start)} ${shiftText(shift)}`
      : `업무 ${totals.pct}% / ${WORK_PASS_RATE}% 이상 퇴근 가능`;
  actionHint.textContent =
    state.fired
      ? "해고 상태 · 자취방에서 다시 준비하기"
      : state.scene === "home"
      ? "WASD/클릭 이동 · 현관문 앞 E 출근 · Tab 핸드폰"
      : "WASD/클릭 이동 · E 처리 · Tab 핸드폰";

  const savingsPct = clamp((state.stats.savings / GOAL) * 100, 0, 100);
  savingsText.textContent = `${money(state.stats.savings)} / ${money(GOAL)}`;
  savingsBar.style.width = `${savingsPct}%`;
  mentalValue.textContent = Math.round(state.stats.mental);
  mentalBar.style.width = `${state.stats.mental}%`;
  staminaValue.textContent = Math.round(state.stats.stamina);
  staminaBar.style.width = `${state.stats.stamina}%`;

  const listTasks = isWorkScene() ? currentScene().tasks : scene.tasks;
  questList.innerHTML = listTasks
    .map((taskItem) => {
      const progress = progressOf(taskItem);
      return `
        <article class="quest-row ${progress >= taskItem.target ? "is-done" : ""}">
          <span class="quest-icon">${taskItem.icon}</span>
          <strong class="quest-name">${taskItem.name}</strong>
          <span class="quest-progress">${Math.min(progress, taskItem.target)} / ${taskItem.target}</span>
        </article>
      `;
    })
    .join("");

  renderMemoryPanel(totals);
  renderBottomPanels();
}

function renderMemoryPanel(totals) {
  if (isWorkScene()) {
    memoryPanel.classList.remove("is-hidden");
    memoryPanel.innerHTML = `
      <h2>암기 미션</h2>
      <div class="memory-list">
        <div class="memory-row"><span>진행</span><strong>업무 처리율</strong><span>${totals.pct}%</span></div>
        <div class="memory-row"><span>조건</span><strong>퇴근 조건</strong><span>${WORK_PASS_RATE}% 이상</span></div>
        <div class="memory-row"><span>변수</span><strong>돌발 변수</strong><span>${state.activeShift?.events || 0}회</span></div>
        ${currentScene().memory
          .slice(0, 4)
          .map(
            ([icon, name, status]) => `
              <div class="memory-row">
                <span>${icon}</span>
                <strong>${name}</strong>
                <span>${status}</span>
              </div>
            `,
          )
          .join("")}
      </div>
    `;
    return;
  }

  memoryPanel.classList.remove("is-hidden");
  memoryPanel.innerHTML = `
    <h2>생활 관리</h2>
    <div class="memory-list">
      <div class="memory-row"><span>월세</span><strong>월세/관리비</strong><span>${money(state.rentDue)}</span></div>
      <div class="memory-row"><span>방</span><strong>방 체크</strong><span>${state.roomChecks}/5</span></div>
      <div class="memory-row"><span>현금</span><strong>보유 현금</strong><span>${money(state.stats.cash)}</span></div>
      <div class="memory-row"><span>경고</span><strong>결근 경고</strong><span>${state.missedShifts}/3</span></div>
    </div>
  `;
}

function renderBottomPanels() {
  logList.innerHTML = state.logs
    .slice(0, 3)
    .map((line) => `<p class="log-line">${line}</p>`)
    .join("");

  summaryList.innerHTML = `
    <div class="summary-row"><span>근무 시간</span><strong>${state.lastShift.hours}</strong></div>
    <div class="summary-row"><span>받은 급여</span><strong>${money(state.lastShift.pay)}</strong></div>
    <div class="summary-row"><span>업무 성공률</span><strong class="plus">${state.lastShift.success}%</strong></div>
    <div class="summary-row"><span>평점 변화</span><strong class="plus">${state.lastShift.boss >= 0 ? "+" : ""}${state.lastShift.boss} / ${state.lastShift.customer >= 0 ? "+" : ""}${state.lastShift.customer}</strong></div>
  `;

  inventoryList.innerHTML = state.inventory
    .map((item) => `<div class="inventory-item" title="${item.name}">${item.icon}<small>${item.count}</small></div>`)
    .join("") || `<div class="inventory-empty">없음</div>`;
}

function drawCover(img) {
  if (!img.complete || !img.naturalWidth) {
    ctx.fillStyle = "#172126";
    ctx.fillRect(0, 0, W, H);
    return;
  }
  const scale = Math.max(W / img.naturalWidth, H / img.naturalHeight);
  const width = img.naturalWidth * scale;
  const height = img.naturalHeight * scale;
  ctx.drawImage(img, (W - width) / 2, (H - height) / 2, width, height);
}

function drawNearestGlow() {
  const taskItem = nearestTask();
  if (!taskItem) return;
  ctx.save();
  ctx.globalAlpha = 0.75;
  ctx.strokeStyle = "#ffcf37";
  ctx.lineWidth = 5;
  ctx.beginPath();
  ctx.arc(taskItem.x, taskItem.y, 28, 0, Math.PI * 2);
  ctx.stroke();
  ctx.restore();
}

function drawTaskMarkers() {
  const near = nearestTask();
  ctx.save();
  ctx.textAlign = "center";

  for (const taskItem of interactiveTasks()) {
    if (taskItem.kind === "clockOut") continue;
    if (isDone(taskItem) && !["schedule", "buyItems"].includes(taskItem.id)) continue;
    const active = near && near.id === taskItem.id;
    const label = `E ${taskItem.icon} ${taskItem.name}`;
    const labelFont = "900 16px Malgun Gothic, sans-serif";
    ctx.font = labelFont;
    const width = Math.max(140, Math.min(285, Math.ceil(ctx.measureText(label).width) + 80));
    const x = clamp(taskItem.x - width / 2, 18, W - width - 18);
    const y = clamp(taskItem.y - 66, 18, H - 76);

    ctx.fillStyle = active ? "rgba(255, 207, 55, 0.98)" : "rgba(255, 220, 92, 0.94)";
    ctx.beginPath();
    ctx.arc(x + 23, y + 22, 20, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = "#141817";
    ctx.font = "900 27px Arial";
    ctx.fillText("!", x + 23, y + 32);

    ctx.fillStyle = active ? "rgba(10, 15, 18, 0.98)" : "rgba(9, 14, 17, 0.92)";
    ctx.strokeStyle = active ? "#ffcf37" : "rgba(255, 220, 92, 0.82)";
    ctx.lineWidth = active ? 4 : 3;
    ctx.beginPath();
    ctx.roundRect(x + 44, y, width - 44, 44, 6);
    ctx.fill();
    ctx.stroke();

    ctx.fillStyle = "#f5f7ef";
    ctx.font = labelFont;
    ctx.fillText(label, x + 44 + (width - 44) / 2, y + 28);
  }

  ctx.restore();
}

function drawDoorPrompt() {
  if (state.scene !== "home") return;
  const active = playerNearDoor();
  ctx.save();
  ctx.globalAlpha = 0.95;
  ctx.strokeStyle = active ? "#ffcf37" : "rgba(255, 207, 55, 0.74)";
  ctx.lineWidth = active ? 5 : 4;
  ctx.strokeRect(doorHotspot.x, doorHotspot.y, doorHotspot.w, doorHotspot.h);

  ctx.fillStyle = active ? "#ffcf37" : "rgba(255, 207, 55, 0.94)";
  ctx.beginPath();
  ctx.arc(doorHotspot.x + doorHotspot.w - 8, doorHotspot.y + 25, 20, 0, Math.PI * 2);
  ctx.fill();
  ctx.fillStyle = "#17110d";
  ctx.font = "900 26px Arial";
  ctx.textAlign = "center";
  ctx.fillText("!", doorHotspot.x + doorHotspot.w - 8, doorHotspot.y + 35);

  ctx.fillStyle = "rgba(5, 10, 13, 0.95)";
  ctx.strokeStyle = active ? "#ffcf37" : "rgba(255, 207, 55, 0.85)";
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.roundRect(doorHotspot.x + 42, doorHotspot.y + 36, 150, 42, 6);
  ctx.fill();
  ctx.stroke();
  ctx.fillStyle = "#f5f7ef";
  ctx.font = "900 18px Malgun Gothic, sans-serif";
  ctx.fillText("E 출근하기", doorHotspot.x + 117, doorHotspot.y + 63);
  ctx.restore();
}

function drawClockOutPrompt() {
  if (!isWorkScene()) return;
  const totals = workTotals();
  ctx.save();
  ctx.fillStyle = "rgba(5, 10, 13, 0.95)";
  ctx.strokeStyle = totals.pct >= WORK_PASS_RATE ? "#ffcf37" : "#687987";
  ctx.lineWidth = 3;
  ctx.beginPath();
  ctx.roundRect(24, 590, 178, 50, 6);
  ctx.fill();
  ctx.stroke();
  ctx.fillStyle = totals.pct >= WORK_PASS_RATE ? "#f5f7ef" : "#a8b7bd";
  ctx.font = "900 18px Malgun Gothic, sans-serif";
  ctx.textAlign = "center";
  ctx.fillText(totals.pct >= WORK_PASS_RATE ? "E 퇴근 정산" : `${WORK_PASS_RATE}% 필요`, 113, 622);
  ctx.restore();
}

function drawPlayer() {
  const { x, y } = state.player;
  ctx.save();
  ctx.fillStyle = "rgba(0,0,0,0.38)";
  ctx.beginPath();
  ctx.ellipse(x, y + 14, 30, 11, 0, 0, Math.PI * 2);
  ctx.fill();

  if (playerImg.complete && playerImg.naturalWidth) {
    const spriteW = 88;
    const spriteH = 116;
    ctx.drawImage(playerImg, x - spriteW / 2, y - spriteH + 16, spriteW, spriteH);
  } else {
    ctx.fillStyle = "#606a6e";
    ctx.fillRect(x - 20, y - 64, 40, 64);
    ctx.fillStyle = "#e8bd94";
    ctx.beginPath();
    ctx.arc(x, y - 74, 19, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.restore();
}

function drawDebugFoot() {
  canvas.dataset.playerX = String(Math.round(state.player.x));
  canvas.dataset.playerY = String(Math.round(state.player.y));
  canvas.dataset.currentScene = state.scene;
}

function draw() {
  ctx.clearRect(0, 0, W, H);
  drawCover(bgImages[state.scene]);
  drawDoorPrompt();
  drawClockOutPrompt();
  drawTaskMarkers();
  drawNearestGlow();
  drawPlayer();
  drawDebugFoot();
}

function update(dt) {
  if (state.commuteOpen || state.scheduleOpen || state.shopOpen) return;

  let dx = 0;
  let dy = 0;
  if (keys.has("up")) dy -= 1;
  if (keys.has("down")) dy += 1;
  if (keys.has("left")) dx -= 1;
  if (keys.has("right")) dx += 1;

  if (dx || dy) {
    state.target = null;
    movePlayer(dx, dy, state.player.speed * dt);
  } else if (state.target) {
    const dxTo = state.target.x - state.player.x;
    const dyTo = state.target.y - state.player.y;
    const distance = Math.hypot(dxTo, dyTo);
    if (distance < 6) state.target = null;
    else movePlayer(dxTo, dyTo, Math.min(distance, state.player.speed * dt));
  }
}

function keyName(event) {
  const value = event.key.toLowerCase();
  if (event.code === "ArrowUp" || event.code === "KeyW" || value === "w") return "up";
  if (event.code === "ArrowDown" || event.code === "KeyS" || value === "s") return "down";
  if (event.code === "ArrowLeft" || event.code === "KeyA" || value === "a") return "left";
  if (event.code === "ArrowRight" || event.code === "KeyD" || value === "d") return "right";
  if (event.code === "KeyE" || value === "e") return "action";
  if (event.code === "Tab") return "phone";
  return "";
}

function focusGame() {
  requestAnimationFrame(() => canvas.focus({ preventScroll: true }));
}

function togglePhone(force = null) {
  if (force !== false && state.commuteOpen) closeCommuteChoice();
  if (force !== false && state.scheduleOpen) closeSchedulePanel();
  if (force !== false && state.shopOpen) closeShopPanel();
  const nextOpen = force === null ? !state.phoneOpen : force;
  if (nextOpen && !state.phoneOpen) state.phoneView = "home";
  state.phoneOpen = nextOpen;
  phone.classList.toggle("is-visible", state.phoneOpen);
  phone.setAttribute("aria-hidden", String(!state.phoneOpen));
  if (state.phoneOpen) renderPhone();
  else focusGame();
}

function fitFrame() {
  const scale = Math.min(window.innerWidth / 1672, window.innerHeight / 941);
  scaleShell.style.width = `${1672 * scale}px`;
  scaleShell.style.height = `${941 * scale}px`;
  gameFrame.style.transform = `scale(${scale})`;
}

function phoneHeader(title) {
  return `
    <div class="phone-topbar">
      ${state.phoneView === "home" ? "<span></span>" : `<button type="button" class="phone-back" data-phone-view="home">‹</button>`}
      <h2>${title}</h2>
      <button type="button" class="phone-close" data-phone-close aria-label="닫기">×</button>
    </div>
  `;
}

function renderPhoneHome() {
  const shift = currentShift();
  const offdayLabel = shift.period === "휴무" ? "오늘 휴무" : "휴무 준비";
  return `
    <div class="phone-app-grid">
      <button class="phone-app" type="button" data-phone-view="bank"><span>₩</span><strong>토스</strong><small>계좌 보기</small></button>
      <button class="phone-app" type="button" data-phone-view="stocks"><span>📈</span><strong>주식</strong><small>국내/해외 거래</small></button>
      <button class="phone-app" type="button" data-phone-view="schedule"><span>📅</span><strong>스케줄</strong><small>근무표 확인</small></button>
      <button class="phone-app" type="button" data-phone-view="offday"><span>🏠</span><strong>${offdayLabel}</strong><small>다음날 준비</small></button>
    </div>
    <div class="phone-card">
      <h3>오늘 상태</h3>
      <p>${clockText()} · ${state.scene === "home" ? "자취방" : `${currentScene().badge} 근무 중`}</p>
      <p>현금 ${money(state.stats.cash)} · 주식 평가손익 ${signedMoney(state.stockProfit)}</p>
    </div>
  `;
}

function renderBankApp() {
  const savingsPct = clamp((state.stats.savings / GOAL) * 100, 0, 100);
  return `
    <div class="bank-hero">
      <span>내 계좌 총합</span>
      <strong>${money(accountTotal())}</strong>
      <small>현금 + 자취자금 + 주식 평가금액</small>
    </div>
    <div class="phone-card bank-goal-card">
      <div class="bank-goal-head">
        <span>자취자금 목표</span>
        <strong>${money(state.stats.savings)} / ${money(GOAL)}</strong>
      </div>
      <div class="wide-meter bank-goal-meter"><span style="width: ${savingsPct}%"></span></div>
      <small>목표까지 ${money(Math.max(0, GOAL - state.stats.savings))} 남음</small>
    </div>
    <div class="phone-card bank-card">
      <div class="bank-row"><span>보유 현금</span><strong>${money(state.stats.cash)}</strong></div>
      <div class="bank-row"><span>자취자금</span><strong>${money(state.stats.savings)}</strong></div>
      <div class="bank-row"><span>주식 평가금액</span><strong>${money(portfolioValue())}</strong></div>
      <div class="bank-row"><span>월세·관리비 남은 금액</span><strong>${money(state.rentDue)}</strong></div>
    </div>
    <div class="phone-card bank-actions">
      <h3>자취자금 저축</h3>
      <div>
        <button type="button" data-bank-action="save" data-save-amount="30000" ${state.stats.cash <= 0 ? "disabled" : ""}>3만원 저축</button>
        <button type="button" data-bank-action="save" data-save-amount="100000" ${state.stats.cash <= 0 ? "disabled" : ""}>10만원 저축</button>
        <button type="button" data-bank-action="save-all" ${state.stats.cash <= 0 ? "disabled" : ""}>남은 현금 전부</button>
      </div>
    </div>
    <div class="phone-card">
      <h3>최근 거래</h3>
      <div class="bank-history">
        ${state.bankHistory
          .map(
            (item) => `
              <div class="bank-row">
                <span>${item.type} · ${item.title}</span>
                <strong class="${item.amount >= 0 ? "plus" : "minus"}">${signedMoney(item.amount)}</strong>
              </div>
            `,
          )
          .join("")}
      </div>
    </div>
  `;
}

function renderStockApp() {
  const visibleStocks = stocks.filter((stock) => stock.market === state.phoneMarket);
  return `
    <div class="stock-summary">
      <div><span>평가금액</span><strong>${money(portfolioValue())}</strong></div>
      <div><span>평가손익</span><strong class="${state.stockProfit >= 0 ? "plus" : "minus"}">${signedMoney(state.stockProfit)}</strong></div>
      <div><span>매수 가능 현금</span><strong>${money(state.stats.cash)}</strong></div>
    </div>
    <div class="stock-tabs">
      <button type="button" class="${state.phoneMarket === "domestic" ? "is-active" : ""}" data-stock-market="domestic">국내</button>
      <button type="button" class="${state.phoneMarket === "overseas" ? "is-active" : ""}" data-stock-market="overseas">해외</button>
    </div>
    <div class="stock-list">
      ${visibleStocks
        .map((stock) => {
          const holdingValue = stock.owned * stock.price;
          const profit = holdingValue - stock.owned * stock.avg;
          return `
            <article class="stock-row">
              <div class="stock-main">
                <strong>${stock.name}</strong>
                <span>${stock.marketName} · ${money(stock.price)} <em class="${stock.change >= 0 ? "plus" : "minus"}">${signedPct(stock.change)}</em></span>
                <small>보유 ${money(holdingValue)} · 손익 <b class="${profit >= 0 ? "plus" : "minus"}">${signedMoney(profit)}</b></small>
              </div>
              <div class="stock-actions">
                <button type="button" data-stock-action="buy" data-stock-id="${stock.id}">1만</button>
                <button type="button" data-stock-action="allin" data-stock-id="${stock.id}" ${state.stats.cash < 1000 ? "disabled" : ""}>올인</button>
                <button type="button" data-stock-action="sell" data-stock-id="${stock.id}" ${stock.owned <= 0 ? "disabled" : ""}>매도</button>
              </div>
            </article>
          `;
        })
        .join("")}
    </div>
    <p class="phone-note">1만은 10,000원 매수, 올인은 보유 현금 전부 매수야. 매도는 해당 종목 전량이고 가격은 게임 시간이 흐를 때마다 변동돼.</p>
  `;
}

function renderScheduleApp() {
  return `
    <div class="phone-card">
      <h3>이번 주 근무 스케줄</h3>
      <ul class="schedule-list">
        ${weeklySchedule
          .map((shift, index) => {
            const marker = index === state.scheduleIndex ? "현재" : "";
            const text = shift.period === "휴무" ? "휴무" : `${timeText(shift.start)} ${shiftText(shift)}`;
            return `<li><span>${marker} ${dayNames[shift.dayIndex]}요일</span><strong>${text}</strong></li>`;
          })
          .join("")}
      </ul>
    </div>
    <div class="phone-card">
      <h3>방 구하기 체크리스트</h3>
      <ul>
        <li>월세와 관리비 분리 확인</li>
        <li>채광, 곰팡이, 방음 확인</li>
        <li>역, 버스정류장 거리 확인</li>
        <li>체크 진행도 ${state.roomChecks}/5</li>
      </ul>
    </div>
  `;
}

function showSchedulePanel() {
  togglePhone(false);
  state.scheduleOpen = true;
  renderSchedulePanel();
  schedulePanel.classList.add("is-visible");
  schedulePanel.setAttribute("aria-hidden", "false");
}

function closeSchedulePanel() {
  state.scheduleOpen = false;
  schedulePanel.classList.remove("is-visible");
  schedulePanel.setAttribute("aria-hidden", "true");
  focusGame();
}

function renderSchedulePanel() {
  const cells = weeklySchedule
    .map((shift, index) => {
      const isCurrent = index === state.scheduleIndex;
      const isRest = shift.duration <= 0;
      const time = isRest ? "휴무" : `${timeText(shift.start)}-${timeText(shift.start + shift.duration)}`;
      const label = isRest ? "자취방 정리" : shiftText(shift);
      return `
        <article class="schedule-cell ${isCurrent ? "is-current" : ""} ${isRest ? "is-rest" : ""}">
          <span class="schedule-day">${dayNames[shift.dayIndex]}요일</span>
          <strong>${time}</strong>
          <em>${label}</em>
          ${isCurrent ? "<b>오늘</b>" : ""}
        </article>
      `;
    })
    .join("");

  schedulePanel.innerHTML = `
    <div class="schedule-card-modal" role="dialog" aria-modal="true" aria-label="이번 주 근무 시간표">
      <button class="schedule-close" type="button" aria-label="닫기">×</button>
      <div class="schedule-pin left"></div>
      <div class="schedule-pin right"></div>
      <h2>이번 주 근무 시간표</h2>
      <p>${clockText()} 기준으로 다음 출근 시간을 확인했다.</p>
      <div class="schedule-board" aria-label="요일별 근무 시간표">
        ${cells}
      </div>
      <div class="schedule-legend">
        <span><i class="work"></i>근무</span>
        <span><i class="rest"></i>휴무</span>
        <span><i class="today"></i>오늘</span>
      </div>
      <button class="schedule-ok" type="button" data-schedule-close>확인</button>
    </div>
  `;
}

function showShopPanel() {
  togglePhone(false);
  state.shopOpen = true;
  renderShopPanel();
  shopPanel.classList.add("is-visible");
  shopPanel.setAttribute("aria-hidden", "false");
}

function closeShopPanel() {
  state.shopOpen = false;
  shopPanel.classList.remove("is-visible");
  shopPanel.setAttribute("aria-hidden", "true");
  focusGame();
}

function renderShopPanel() {
  shopPanel.innerHTML = `
    <div class="shop-card-modal" role="dialog" aria-modal="true" aria-label="자취 아이템 구매">
      <button class="shop-close" type="button" aria-label="닫기">×</button>
      <div class="shop-header">
        <span>편의점 장보기</span>
        <h2>자취 필수템 구매</h2>
        <strong>보유 현금 ${money(state.stats.cash)}</strong>
      </div>
      <div class="shop-grid">
        ${shopItems
          .map(
            (item) => `
              <button class="shop-item" type="button" data-shop-item="${item.id}" ${state.stats.cash < item.price ? "disabled" : ""}>
                <span class="shop-icon">${item.icon}</span>
                <strong>${item.name}</strong>
                <em>${money(item.price)}</em>
                <small>${item.desc}</small>
              </button>
            `,
          )
          .join("")}
      </div>
      <p class="shop-note">구매한 물건은 보유 아이템에 쌓이고, 토스 앱 거래내역에도 지출로 남는다.</p>
      <button class="shop-ok" type="button" data-shop-close>닫기</button>
    </div>
  `;
}

function renderOffdayApp() {
  const shift = currentShift();
  return `
    <div class="phone-card">
      <h3>${shift.period === "휴무" ? "휴무일 추천 루틴" : "다음 휴무 때 할 일"}</h3>
      <ul>
        ${offdaySuggestions().map((item) => `<li>${item}</li>`).join("")}
      </ul>
    </div>
    <div class="phone-card">
      <h3>추가하면 재미있는 휴무 콘텐츠</h3>
      <p>휴무일 전용 선택지로 산책, 장보기, 방 청소, 자격증 공부, 친구 연락을 넣으면 멘탈/체력/자취자금이 다르게 변해서 더 현실 시뮬레이션 느낌이 나.</p>
    </div>
  `;
}

function renderPhone() {
  const titleMap = {
    home: "알바폰",
    bank: "토스",
    stocks: "주식",
    schedule: "스케줄",
    offday: "휴무 추천",
  };
  const bodyMap = {
    home: renderPhoneHome,
    bank: renderBankApp,
    stocks: renderStockApp,
    schedule: renderScheduleApp,
    offday: renderOffdayApp,
  };
  const view = bodyMap[state.phoneView] ? state.phoneView : "home";
  phone.innerHTML = `
    <div class="phone-screen">
      ${phoneHeader(titleMap[view])}
      ${bodyMap[view]()}
    </div>
  `;
}

function loop(now) {
  const dt = Math.min(0.034, (now - loop.last) / 1000 || 0.016);
  loop.last = now;
  update(dt);
  draw();
  requestAnimationFrame(loop);
}

loop.last = performance.now();

window.addEventListener(
  "keydown",
  (event) => {
    if (state.scheduleOpen) {
      if (event.key === "Escape") closeSchedulePanel();
      event.preventDefault();
      return;
    }

    if (state.shopOpen) {
      if (event.key === "Escape") closeShopPanel();
      event.preventDefault();
      return;
    }

    if (state.commuteOpen) {
      if (event.key === "Escape") closeCommuteChoice();
      event.preventDefault();
      return;
    }

    if (event.key === "Escape" && state.phoneOpen) {
      togglePhone(false);
      event.preventDefault();
      return;
    }

    const key = keyName(event);
    if (!key) return;
    if (["up", "down", "left", "right"].includes(key)) {
      keys.add(key);
      event.preventDefault();
    }
    if (key === "action") {
      interact();
      event.preventDefault();
    }
    if (key === "phone") {
      togglePhone();
      event.preventDefault();
    }
  },
  true,
);

window.addEventListener(
  "keyup",
  (event) => {
    const key = keyName(event);
    if (key) keys.delete(key);
  },
  true,
);

canvas.addEventListener("pointerdown", (event) => {
  if (state.commuteOpen || state.scheduleOpen || state.shopOpen) return;
  focusGame();
  const box = canvas.getBoundingClientRect();
  const point = {
    x: ((event.clientX - box.left) / box.width) * W,
    y: ((event.clientY - box.top) / box.height) * H,
  };

  if (state.scene === "home" && pointInRect(point, doorHotspot)) {
    state.player = { ...state.player, x: 164, y: 285 };
    showCommuteChoice();
    return;
  }

  if (isWorkScene() && pointInRect(point, clockOutHotspot)) {
    finishShift();
    return;
  }

  const taskItem = pointedTask(point);
  state.target = taskItem ? { x: taskItem.x, y: taskItem.y } : point;
});

commutePanel.addEventListener("pointerdown", (event) => {
  event.stopPropagation();
  const closeButton = event.target.closest(".commute-close");
  const optionButton = event.target.closest("[data-commute]");
  if (closeButton) {
    closeCommuteChoice();
    return;
  }
  if (optionButton) {
    startCommute(optionButton.dataset.commute);
    return;
  }
  if (event.target === commutePanel) closeCommuteChoice();
});

schedulePanel.addEventListener("pointerdown", (event) => {
  event.stopPropagation();
  if (event.target.closest(".schedule-close") || event.target.closest("[data-schedule-close]")) {
    closeSchedulePanel();
    return;
  }
  if (event.target === schedulePanel) closeSchedulePanel();
});

shopPanel.addEventListener("pointerdown", (event) => {
  event.stopPropagation();
  const closeButton = event.target.closest(".shop-close");
  const closeAction = event.target.closest("[data-shop-close]");
  const itemButton = event.target.closest("[data-shop-item]");

  if (closeButton || closeAction) {
    closeShopPanel();
    return;
  }

  if (itemButton) {
    buyShopItem(itemButton.dataset.shopItem);
    return;
  }

  if (event.target === shopPanel) closeShopPanel();
});

phone.addEventListener("click", (event) => {
  const closeButton = event.target.closest("[data-phone-close]");
  const viewButton = event.target.closest("[data-phone-view]");
  const marketButton = event.target.closest("[data-stock-market]");
  const stockButton = event.target.closest("[data-stock-action]");
  const bankButton = event.target.closest("[data-bank-action]");

  if (closeButton) {
    togglePhone(false);
    return;
  }

  if (viewButton) {
    state.phoneView = viewButton.dataset.phoneView;
    renderPhone();
    return;
  }

  if (marketButton) {
    state.phoneMarket = marketButton.dataset.stockMarket;
    renderPhone();
    return;
  }

  if (stockButton) {
    const action = stockButton.dataset.stockAction;
    const id = stockButton.dataset.stockId;
    if (action === "buy") buyStock(id);
    if (action === "allin") buyStock(id, "allin");
    if (action === "sell") sellStock(id);
    return;
  }

  if (bankButton) {
    const action = bankButton.dataset.bankAction;
    if (action === "save") transferToSavings(Number(bankButton.dataset.saveAmount) || 0);
    if (action === "save-all") transferToSavings(state.stats.cash);
  }
});

document.querySelector(".game-frame").addEventListener("pointerdown", focusGame);
window.addEventListener("blur", () => keys.clear());
window.addEventListener("resize", fitFrame);

fitFrame();
renderUi();
draw();
focusGame();
requestAnimationFrame(loop);
