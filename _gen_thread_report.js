const fs = require('fs');
const { Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
        AlignmentType, LevelFormat, HeadingLevel, BorderStyle, WidthType,
        ShadingType, PageNumber, Header, Footer, PageBreak } = require('docx');

// ── 编号样式 ──
const numbering = {
  config: [
    {
      reference: "bullets",
      levels: [{
        level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 720, hanging: 360 } } }
      }]
    },
    {
      reference: "numbers",
      levels: [{
        level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT,
        style: { paragraph: { indent: { left: 720, hanging: 360 } } }
      }]
    }
  ]
};

// ── 通用 ──
const border = { style: BorderStyle.SINGLE, size: 1, color: "BBBBBB" };
const borders = { top: border, bottom: border, left: border, right: border };
const cellMargins = { top: 80, bottom: 80, left: 120, right: 120 };
const CODE_FONT = "Consolas";
const BODY_FONT = "Arial";

function p(text, opts = {}) {
  const runs = [];
  if (typeof text === 'string') {
    runs.push(new TextRun({ text, font: BODY_FONT, size: 22, bold: !!opts.bold, color: opts.color || "000000" }));
  } else {
    text.forEach(t => runs.push(t));
  }
  return new Paragraph({
    spacing: { after: opts.after !== undefined ? opts.after : 120, before: opts.before || 0 },
    alignment: opts.align || AlignmentType.LEFT,
    children: runs,
    heading: opts.heading || undefined,
    numbering: opts.numbering || undefined,
  });
}

function code(text) {
  return new TextRun({ text, font: CODE_FONT, size: 18, color: "333333" });
}

function codeP(text) {
  return new Paragraph({
    spacing: { after: 60, before: 0 },
    indent: { left: 360 },
    children: [code(text)],
  });
}

function h1(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_1,
    spacing: { before: 360, after: 160 },
    children: [new TextRun({ text, font: BODY_FONT, size: 32, bold: true, color: "000000" })]
  });
}
function h2(text) {
  return new Paragraph({
    heading: HeadingLevel.HEADING_2,
    spacing: { before: 280, after: 120 },
    children: [new TextRun({ text, font: BODY_FONT, size: 26, bold: true, color: "1A1A1A" })]
  });
}

function cell(text, opts = {}) {
  const runs = typeof text === 'string'
    ? [new TextRun({ text, font: BODY_FONT, size: 20, bold: !!opts.bold, color: opts.color || "000000" })]
    : text;
  return new TableCell({
    borders,
    margins: cellMargins,
    width: { size: opts.width || 2340, type: WidthType.DXA },
    shading: opts.shading ? { fill: opts.shading, type: ShadingType.CLEAR } : undefined,
    children: [new Paragraph({ spacing: { after: 40 }, children: runs })],
  });
}

function row(cells) {
  return new TableRow({ children: cells });
}

function table(rows, colWidths) {
  const total = colWidths.reduce((a, b) => a + b, 0);
  return new Table({
    width: { size: total, type: WidthType.DXA },
    columnWidths: colWidths,
    rows: rows,
  });
}

// ── 文档主体 ──
const children = [];

// 标题
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { after: 60 },
  children: [new TextRun({ text: "后端线程管理问题修复报告", font: BODY_FONT, size: 40, bold: true, color: "000000" })]
}));
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { after: 80 },
  children: [new TextRun({ text: "Spotify Remake 项目 — 项目痛点", font: BODY_FONT, size: 22, color: "666666" })]
}));
children.push(new Paragraph({
  alignment: AlignmentType.CENTER,
  spacing: { after: 200 },
  children: [new TextRun({ text: "日期：2026-06-28", font: BODY_FONT, size: 20, color: "888888" })]
}));

// ═══ 1. 概述 ═══
children.push(h1("一、概述"));
children.push(p("在对后端代码进行线程安全审查时，发现 3 个与线程管理相关的隐患，涉及 ThreadLocal 内存泄漏、线程池未关闭、以及 CompletableFuture 滥用公共 ForkJoinPool。以下逐项分析并给出修复方案。"));
children.push(p("影响范围：4 个文件，均为后端 Java 代码。"));

// ═══ 2. 问题一：ThreadLocal 内存泄漏 ═══
children.push(h1("二、问题一：ThreadLocal 内存泄漏（严重）"));
children.push(h2("2.1 问题描述"));
children.push(p("BaseContext 工具类使用 ThreadLocal<Long> 存储当前请求的用户 ID，提供了 setCurrentId()、getCurrentId()、removeCurrentId() 三个方法。"));
children.push(p("TokenInterceptor 和 AdminTokenInterceptor 在 preHandle() 中调用 setCurrentId() 设置了用户 ID，但在请求结束后从未调用 removeCurrentId() 进行清理。"));
children.push(h2("2.2 影响"));
children.push(p("Tomcat 使用线程池复用线程。如果 ThreadLocal 不被清理，上一个请求的用户 ID 会残留在该线程中。下一个分配到同一线程的请求可能错误地读取到上一个用户的数据，造成数据错乱。此外，在应用热部署时，ThreadLocal 引用会阻止 ClassLoader 被 GC，导致元空间泄漏。"));
children.push(h2("2.3 修复方案"));
children.push(p("在两个拦截器中添加 afterCompletion() 方法，调用 BaseContext.removeCurrentId() 进行清理。"));
children.push(h2("2.4 修复文件"));
children.push(p("TokenInterceptor.java — 新增 afterCompletion 方法", { numbering: { reference: "numbers", level: 0 } }));
children.push(p("AdminTokenInterceptor.java — 新增 afterCompletion 方法", { numbering: { reference: "numbers", level: 0 } }));
children.push(p("修复后代码："));
children.push(codeP("@Override"));
children.push(codeP("public void afterCompletion(HttpServletRequest request,"));
children.push(codeP("        HttpServletResponse response, Object handler, Exception ex) {"));
children.push(codeP("    BaseContext.removeCurrentId();"));
children.push(codeP("}"));

// ═══ 3. 问题二：ScheduledExecutorService 未关闭 ═══
children.push(h1("三、问题二：ScheduledExecutorService 未关闭（中等）"));
children.push(h2("3.1 问题描述"));
children.push(p("LocalCaptchaCache 类在静态初始化块中创建了一个 ScheduledExecutorService（单线程调度器），用于每 5 秒清理过期的验证码缓存。但该类没有提供任何关闭该线程池的机制。"));
children.push(h2("3.2 影响"));
children.push(p("应用关闭时，后台清理线程不会被终止，导致："));
children.push(p("非守护线程阻止 JVM 正常退出", { numbering: { reference: "bullets", level: 0 } }));
children.push(p("在容器环境（如 Tomcat）中可能产生内存泄漏警告", { numbering: { reference: "bullets", level: 0 } }));
children.push(p("多次热部署会累积僵尸线程", { numbering: { reference: "bullets", level: 0 } }));
children.push(h2("3.3 修复方案"));
children.push(p("在静态初始化块中注册 JVM Shutdown Hook，在 JVM 关闭时优雅地 shutdown 线程池。注意：由于该类是静态工具类（非 Spring Bean），不能使用 @PreDestroy 注解。"));
children.push(h2("3.4 修复文件"));
children.push(p("LocalCaptchaCache.java — 在 static 块中添加 Runtime.getRuntime().addShutdownHook()", { numbering: { reference: "numbers", level: 0 } }));

// ═══ 4. 问题三：CompletableFuture 使用公共 ForkJoinPool ═══
children.push(h1("四、问题三：CompletableFuture 使用公共 ForkJoinPool（中等）"));
children.push(h2("4.1 问题描述"));
children.push(p("GDMusicApiClient 是外部音乐 API 客户端，在 search() 和 searchByArtist() 两个方法中使用了 CompletableFuture.runAsync() 和 CompletableFuture.supplyAsync() 来并行获取封面图片和搜索多个音乐源。"));
children.push(p("但这些异步调用均未传入自定义 Executor，默认使用 ForkJoinPool.commonPool()。该公共池的并行度通常等于 CPU 核心数 - 1，且被 JVM 中所有代码共享。"));
children.push(h2("4.2 影响"));
children.push(p("GDMusicApiClient 中的异步任务执行的是阻塞 HTTP 请求。阻塞任务占用公共 ForkJoinPool 线程会导致："));
children.push(p("其他依赖 commonPool 的组件（如 parallelStream）性能下降或饥饿", { numbering: { reference: "bullets", level: 0 } }));
children.push(p("高并发场景下 commonPool 线程被占满，导致请求排队超时", { numbering: { reference: "bullets", level: 0 } }));
children.push(p("违反 \"公共线程池不执行阻塞操作\" 的 Java 并发最佳实践", { numbering: { reference: "bullets", level: 0 } }));
children.push(h2("4.3 修复方案"));
children.push(p("注入自定义 ExecutorService（使用 Executors.newCachedThreadPool()），所有 CompletableFuture 异步调用显式传入该 Executor。同时添加 @PreDestroy 方法在 Bean 销毁时关闭线程池。"));
children.push(p("涉及修改的异步调用位置（共 3 处）："));
children.push(p("search() 中封面获取的 runAsync", { numbering: { reference: "numbers", level: 0 } }));
children.push(p("searchByArtist() 中多源搜索的 supplyAsync", { numbering: { reference: "numbers", level: 0 } }));
children.push(p("searchByArtist() 中封面获取的 runAsync", { numbering: { reference: "numbers", level: 0 } }));
children.push(h2("4.4 修复文件"));
children.push(p("GDMusicApiClient.java — 添加 ExecutorService 字段、@PreDestroy 方法、修改 3 处异步调用", { numbering: { reference: "numbers", level: 0 } }));

// ═══ 5. 汇总 ═══
children.push(h1("五、修复汇总"));

const totalWidth = 9360;
const col1 = 1200;
const col2 = 2960;
const col3 = 2400;
const col4 = 2800;

const headerShade = "D5E8F0";
children.push(table([
  row([
    cell("序号",   { bold: true, width: col1, shading: headerShade }),
    cell("文件",   { bold: true, width: col2, shading: headerShade }),
    cell("问题类型", { bold: true, width: col3, shading: headerShade }),
    cell("修复方式", { bold: true, width: col4, shading: headerShade }),
  ]),
  row([
    cell("1", { width: col1 }),
    cell("TokenInterceptor.java", { width: col2 }),
    cell("ThreadLocal 泄漏", { width: col3 }),
    cell("新增 afterCompletion() 清理", { width: col4 }),
  ]),
  row([
    cell("2", { width: col1 }),
    cell("AdminTokenInterceptor.java", { width: col2 }),
    cell("ThreadLocal 泄漏", { width: col3 }),
    cell("新增 afterCompletion() 清理", { width: col4 }),
  ]),
  row([
    cell("3", { width: col1 }),
    cell("LocalCaptchaCache.java", { width: col2 }),
    cell("线程池未关闭", { width: col3 }),
    cell("添加 JVM Shutdown Hook", { width: col4 }),
  ]),
  row([
    cell("4", { width: col1 }),
    cell("GDMusicApiClient.java", { width: col2 }),
    cell("滥用公共 ForkJoinPool", { width: col3 }),
    cell("自定义 CachedThreadPool + @PreDestroy", { width: col4 }),
  ]),
], [col1, col2, col3, col4]));

children.push(new Paragraph({ spacing: { before: 200 }, children: [] }));

// ═══ 6. 最佳实践建议 ═══
children.push(h1("六、后续建议"));
children.push(p("所有自定义 ExecutorService 应统一管理，可抽取为 Spring Bean 由容器统一注入和关闭。", { numbering: { reference: "numbers", level: 0 } }));
children.push(p("规范 ThreadLocal 使用模式：set 和 remove 必须成对出现，建议封装到 try-finally 或拦截器的 afterCompletion 中。", { numbering: { reference: "numbers", level: 0 } }));
children.push(p("禁止在公共 ForkJoinPool 上执行阻塞 I/O 操作，阻塞任务一律使用自定义线程池。", { numbering: { reference: "numbers", level: 0 } }));
children.push(p("考虑使用 ThreadPoolTaskExecutor（Spring 提供）替代 Executors 工厂方法，获得更好的监控和管理能力。", { numbering: { reference: "numbers", level: 0 } }));

// ── 构建文档 ──
const doc = new Document({
  styles: {
    default: { document: { run: { font: BODY_FONT, size: 22 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 32, bold: true, font: BODY_FONT },
        paragraph: { spacing: { before: 360, after: 160 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 26, bold: true, font: BODY_FONT },
        paragraph: { spacing: { before: 280, after: 120 }, outlineLevel: 1 } },
    ]
  },
  numbering,
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 },
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
      }
    },
    headers: {
      default: new Header({
        children: [new Paragraph({
          alignment: AlignmentType.RIGHT,
          children: [new TextRun({ text: "线程管理问题修复报告", font: BODY_FONT, size: 16, color: "999999", italics: true })]
        })]
      })
    },
    footers: {
      default: new Footer({
        children: [new Paragraph({
          alignment: AlignmentType.CENTER,
          children: [
            new TextRun({ text: "- ", font: BODY_FONT, size: 16, color: "999999" }),
            new TextRun({ children: [PageNumber.CURRENT], font: BODY_FONT, size: 16, color: "999999" }),
            new TextRun({ text: " -", font: BODY_FONT, size: 16, color: "999999" }),
          ]
        })]
      })
    },
    children,
  }]
});

const OUT = "D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/项目痛点/后端线程管理问题修复报告.docx";
Packer.toBuffer(doc).then(buf => {
  fs.writeFileSync(OUT, buf);
  console.log("OK: " + OUT);
}).catch(e => {
  console.error(e);
  process.exit(1);
});
