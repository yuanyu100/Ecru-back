const escapeHtml = (value = '') =>
  String(value)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');

const formatInline = (value = '') =>
  escapeHtml(value)
    .replace(/`([^`]+)`/g, '<code>$1</code>')
    .replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>')
    .replace(/\*([^*\n]+)\*/g, '<em>$1</em>');

export const formatMessageHtml = (content = '') => {
  const normalized = String(content || '').replace(/\r\n/g, '\n').trim();
  if (!normalized) {
    return '';
  }

  const lines = normalized.split('\n');
  const chunks = [];
  let paragraph = [];
  let listType = null;
  let listItems = [];

  const flushParagraph = () => {
    if (!paragraph.length) {
      return;
    }
    chunks.push(`<p>${paragraph.map(formatInline).join('<br />')}</p>`);
    paragraph = [];
  };

  const flushList = () => {
    if (!listType || !listItems.length) {
      listType = null;
      listItems = [];
      return;
    }
    chunks.push(
      `<${listType}>${listItems.map((item) => `<li>${formatInline(item)}</li>`).join('')}</${listType}>`
    );
    listType = null;
    listItems = [];
  };

  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line) {
      flushParagraph();
      flushList();
      continue;
    }

    const orderedMatch = line.match(/^\d+\.\s+(.+)$/);
    if (orderedMatch) {
      flushParagraph();
      if (listType && listType !== 'ol') {
        flushList();
      }
      listType = 'ol';
      listItems.push(orderedMatch[1]);
      continue;
    }

    const unorderedMatch = line.match(/^[-*]\s+(.+)$/);
    if (unorderedMatch) {
      flushParagraph();
      if (listType && listType !== 'ul') {
        flushList();
      }
      listType = 'ul';
      listItems.push(unorderedMatch[1]);
      continue;
    }

    flushList();
    paragraph.push(line);
  }

  flushParagraph();
  flushList();

  return chunks.join('');
};

