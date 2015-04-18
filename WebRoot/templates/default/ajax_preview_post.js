$("#previewSubject").html("${post.subject?html}");
$("#previewMessage").html("${post.text}");
$("#previewTable").show();

var s = document.location.toString();
var index = s.indexOf("#preview");

if (index > -1) {
	s = s.substring(0, index);
}

document.location = s + "#preview";

SyntaxHighlighter.config.clipboardSwf = '${contextPath}/javascript/clipboard.swf';
SyntaxHighlighter.all();
dp.SyntaxHighlighter.HighlightAll('code');
