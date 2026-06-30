const fs = require('fs');
const { Document, Packer, Paragraph, TextRun, HeadingLevel, AlignmentType, LevelFormat, BorderStyle, ShadingType, WidthType, Table, TableRow, TableCell } = require('docx');

const doc = new Document({
  styles: {
    default: { document: { run: { font: "Arial", size: 24 } } },
    paragraphStyles: [
      { id: "Heading1", name: "Heading 1", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 36, bold: true, font: "Arial", color: "1A1A1A" },
        paragraph: { spacing: { before: 360, after: 240 }, outlineLevel: 0 } },
      { id: "Heading2", name: "Heading 2", basedOn: "Normal", next: "Normal", quickFormat: true,
        run: { size: 28, bold: true, font: "Arial", color: "333333" },
        paragraph: { spacing: { before: 240, after: 180 }, outlineLevel: 1 } },
    ]
  },
  numbering: {
    config: [
      { reference: "bullets",
        levels: [{ level: 0, format: LevelFormat.BULLET, text: "•", alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
      { reference: "numbers",
        levels: [{ level: 0, format: LevelFormat.DECIMAL, text: "%1.", alignment: AlignmentType.LEFT,
          style: { paragraph: { indent: { left: 720, hanging: 360 } } } }] },
    ]
  },
  sections: [{
    properties: {
      page: {
        size: { width: 11906, height: 16838 }, // A4
        margin: { top: 1440, right: 1440, bottom: 1440, left: 1440 }
      }
    },
    children: [
      // Title
      new Paragraph({
        heading: HeadingLevel.HEADING_1,
        alignment: AlignmentType.CENTER,
        children: [new TextRun("外部歌曲播放进度条无法拖动修复报告")]
      }),
      new Paragraph({
        alignment: AlignmentType.CENTER,
        spacing: { after: 360 },
        children: [new TextRun({ text: "2026-06-27", size: 20, color: "888888" })]
      }),

      // Section 1
      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("一、问题现象")] }),
      new Paragraph({
        spacing: { after: 120 },
        children: [new TextRun("播放外部歌曲（GD Music 等外部源）时，底部播放栏的进度条无法拖动，进度条始终显示 0%，点击或拖拽进度条无任何反应。本地歌曲正常。")]
      }),

      // Section 2
      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("二、问题根因")] }),
      new Paragraph({
        spacing: { after: 120 },
        children: [new TextRun("问题由后端和前端两个层面叠加导致：")]
      }),

      new Paragraph({
        spacing: { after: 60 },
        children: [new TextRun({ text: "1. 后端 ExternalMusicController.streamProxy() 代理外部音频流时：", bold: true })]
      }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("没有转发上游的 Content-Length 响应头，浏览器无法从无长度流中确定音频总时长")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("没有处理 HTTP Range 请求头，浏览器无法执行 seek 操作")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 }, children: [new TextRun("导致 HTMLAudioElement.duration 变为 Infinity，且拒绝 currentTime 赋值")] }),

      new Paragraph({
        spacing: { after: 60 },
        children: [new TextRun({ text: "2. 前端 MainApp.vue 中 3 处 loadedmetadata 事件处理器：", bold: true })]
      }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("playSong()（本地歌曲）、playExternalSongFromPlaylist()、playExternalTrack()")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("均无条件执行 currentSong.value.duration = audio.value.duration")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("将歌曲元数据中正确的时长覆盖为 Infinity")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("导致 progressPercent = currentTime / Infinity = 0（进度条始终 0%）")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 }, children: [new TextRun("导致 seekBy() 计算 pct * Infinity = NaN，audio.currentTime = NaN 无效果")] }),

      // Section 3
      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("三、修复方案")] }),

      new Paragraph({
        spacing: { after: 60 },
        children: [new TextRun({ text: "后端修复（ExternalMusicController.java）：", bold: true })]
      }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("新增 HttpServletRequest 参数，读取客户端 Range 请求头并转发到上游 CDN")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("转发上游响应中的 Content-Length、Accept-Ranges、Content-Range 头")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("支持 206 Partial Content 状态码（之前只接受 200）")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 }, children: [new TextRun("重定向跟随过程中也保持 Range 头的转发")] }),

      new Paragraph({
        spacing: { after: 60 },
        children: [new TextRun({ text: "前端修复（MainApp.vue，3 处）：", bold: true })]
      }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("在 loadedmetadata 处理器中增加 isFinite(ad) && ad > 0 守卫")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, children: [new TextRun("仅当 audio.value.duration 是有效有限正数时才覆盖 currentSong.value.duration")] }),
      new Paragraph({ numbering: { reference: "bullets", level: 0 }, spacing: { after: 60 }, children: [new TextRun("保护歌曲元数据中的正确时长不被 Infinity 覆盖")] }),

      // Section 4
      new Paragraph({ heading: HeadingLevel.HEADING_2, children: [new TextRun("四、影响范围")] }),

      // Table
      new Table({
        width: { size: 9026, type: WidthType.DXA },
        columnWidths: [2000, 7026],
        rows: [
          new TableRow({
            children: [
              new TableCell({
                width: { size: 2000, type: WidthType.DXA },
                shading: { fill: "F0F0F0", type: ShadingType.CLEAR },
                margins: { top: 80, bottom: 80, left: 120, right: 120 },
                children: [new Paragraph({ children: [new TextRun({ text: "层级", bold: true })] })]
              }),
              new TableCell({
                width: { size: 7026, type: WidthType.DXA },
                shading: { fill: "F0F0F0", type: ShadingType.CLEAR },
                margins: { top: 80, bottom: 80, left: 120, right: 120 },
                children: [new Paragraph({ children: [new TextRun({ text: "涉及文件及方法", bold: true })] })]
              }),
            ]
          }),
          new TableRow({
            children: [
              new TableCell({
                width: { size: 2000, type: WidthType.DXA },
                margins: { top: 80, bottom: 80, left: 120, right: 120 },
                children: [new Paragraph({ children: [new TextRun("后端")] })]
              }),
              new TableCell({
                width: { size: 7026, type: WidthType.DXA },
                margins: { top: 80, bottom: 80, left: 120, right: 120 },
                children: [new Paragraph({ children: [new TextRun("ExternalMusicController.java — streamProxy 方法")] })]
              }),
            ]
          }),
          new TableRow({
            children: [
              new TableCell({
                width: { size: 2000, type: WidthType.DXA },
                margins: { top: 80, bottom: 80, left: 120, right: 120 },
                children: [new Paragraph({ children: [new TextRun("前端")] })]
              }),
              new TableCell({
                width: { size: 7026, type: WidthType.DXA },
                margins: { top: 80, bottom: 80, left: 120, right: 120 },
                children: [
                  new Paragraph({ children: [new TextRun("MainApp.vue — playSong 的 loadedmetadata 回调")] }),
                  new Paragraph({ children: [new TextRun("MainApp.vue — playExternalSongFromPlaylist 的 loadedmetadata 回调")] }),
                  new Paragraph({ children: [new TextRun("MainApp.vue — playExternalTrack 的 loadedmetadata 回调")] }),
                ]
              }),
            ]
          }),
        ]
      }),

      // Section 5
      new Paragraph({ heading: HeadingLevel.HEADING_2, spacing: { before: 360 }, children: [new TextRun("五、验证方法")] }),
      new Paragraph({ numbering: { reference: "numbers", level: 0 }, children: [new TextRun("重启后端服务")] }),
      new Paragraph({ numbering: { reference: "numbers", level: 0 }, children: [new TextRun("在前端搜索外部歌曲（GD Music 源）")] }),
      new Paragraph({ numbering: { reference: "numbers", level: 0 }, children: [new TextRun("播放外部歌曲，观察进度条是否正常走动")] }),
      new Paragraph({ numbering: { reference: "numbers", level: 0 }, children: [new TextRun("拖拽进度条，确认能正常 seek 到指定位置")] }),
      new Paragraph({ numbering: { reference: "numbers", level: 0 }, spacing: { after: 120 }, children: [new TextRun("同时验证本地歌曲进度条不受影响")] }),
    ]
  }]
});

Packer.toBuffer(doc).then(buffer => {
  const outPath = 'D:/javaedit/project/spotify/Spotify_remake/Spotify_Remake/项目痛点/外部歌曲进度条无法拖动修复报告.docx';
  fs.writeFileSync(outPath, buffer);
  console.log('OK: ' + outPath);
});
