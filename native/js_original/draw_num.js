importPackage(java.awt);

const SANS_LAO = Resources.readFont(Resources.idr("fonts/sans_lao/sans_lao.ttf"));
const SOURCE_HAN_SANS_CN = Resources.readFont(Resources.idr("fonts/source-han-sans-cn/source-han-sans-cn-regular.otf"));
const NUM_WIDTH = 1120;
const NUM_HEIGHT = 240;
const ERROR = "请检查侧线名。|Please check the siding name."

function drawNum(g, num, fontSize) {
    if (!g) return;
    var font = SANS_LAO.deriveFont(fontSize);
    var fm = g.getFontMetrics(font);
    var str = "" + num;                       // 强制转原生 JS 字符串
    var width = (NUM_WIDTH - fm.stringWidth(str)) / 2;
    var height = NUM_HEIGHT - 12;
    g.setColor(java.awt.Color.BLACK);         // ← 用常量，避免 3-arg 浮点构造越界
    g.setFont(font);
    g.drawString(str, width, height);
    print("编号" + str + "绘制成功！宽度：" + width + "，高度：" + height + "。");
}

function drawError(g) {
    if (!g) return;
    var font = SOURCE_HAN_SANS_CN.deriveFont(60);
    var fm = g.getFontMetrics(font);
    var width = (NUM_WIDTH - fm.stringWidth(ERROR)) / 2;
    var height = NUM_HEIGHT - 12;
    g.setColor(java.awt.Color.RED);           // ← 用常量
    g.setFont(font);
    g.drawString(ERROR, width, height);
    print(ERROR);
}