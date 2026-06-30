#!/usr/bin/env python3
"""
Fix GBK/UTF-8 double-encoding corruption.

Corruption chain: Original UTF-8 bytes -> misinterpreted as GBK -> saved as UTF-8
Fix: Garbled CJK blocks -> encode to GBK bytes -> decode as UTF-8

Safety: only apply fix if result is "better" — i.e. doesn't introduce
Greek/Cyrillic/other non-CJK scripts, and fixed text has more common CJK chars.
"""

import os
import sys

# Common CJK Unified Ideographs range
CJK_BEGIN = 0x4E00
CJK_END = 0x9FFF
# CJK Extension A
CJK_EXT_A_BEGIN = 0x3400
CJK_EXT_A_END = 0x4DBF


def is_common_cjk(cp):
    """Check if code point is in common CJK range."""
    return (CJK_BEGIN <= cp <= CJK_END) or (CJK_EXT_A_BEGIN <= cp <= CJK_EXT_A_END)


def is_suspicious_char(c):
    """Characters that should NOT appear in valid Chinese text."""
    cp = ord(c)
    # Greek and Coptic
    if 0x0370 <= cp <= 0x03FF:
        return True
    # Cyrillic
    if 0x0400 <= cp <= 0x04FF:
        return True
    # Various other non-CJK scripts that could result from false fix
    if 0x0100 <= cp <= 0x036F:  # Latin Extended, IPA
        return True
    if 0x1E00 <= cp <= 0x1EFF:  # Latin Extended Additional
        return True
    if 0x2000 <= cp <= 0x206F:  # General Punctuation (some)
        return True
    if 0x2100 <= cp <= 0x214F:  # Letterlike Symbols
        return True
    return False


def count_cjk(text):
    """Count common CJK characters in text."""
    return sum(1 for c in text if is_common_cjk(ord(c)))


def is_valid_fix(original, fixed):
    """
    Check if the fix actually improved the text.
    - Fixed text must not contain suspicious characters (Greek, Cyrillic, etc.)
    - Fixed text must still contain common CJK characters (we recovered Chinese)
    - If original was all common CJK, fixed should not introduce non-CJK scripts
    """
    # If fixed text contains suspicious non-CJK characters, reject
    for c in fixed:
        if is_suspicious_char(c):
            return False

    orig_cjk = count_cjk(original)
    fixed_cjk = count_cjk(fixed)

    # If fixed has NO common CJK chars but original had some, the fix degraded the text
    # (e.g. Chinese chars became non-Chinese)
    if orig_cjk > 0 and fixed_cjk == 0:
        return False

    # If neither has CJK, no improvement
    if orig_cjk == 0 and fixed_cjk == 0:
        return False

    # Fixed has CJK chars and no suspicious chars — looks good
    return True


def fix_block(block):
    """Try to fix a CJK block. Returns fixed text, or None if fix fails."""
    if count_cjk(block) == 0:
        return None
    try:
        gbk_bytes = block.encode('gbk')
        fixed = gbk_bytes.decode('utf-8')
        if is_valid_fix(block, fixed):
            return fixed
    except (UnicodeEncodeError, UnicodeDecodeError):
        pass
    return None


def fix_text(text):
    """Fix garbled CJK blocks with validation.

    Two passes:
    1. Per-block fix (ASCII acts as block boundary)
    2. For remaining garbled lines, try whole-line fix
    """
    # Pass 1: per-block fix
    result = []
    i = 0
    while i < len(text):
        c = text[i]
        cp = ord(c)

        # ASCII and Latin-1 stay as-is
        if cp < 256:
            result.append(c)
            i += 1
            continue

        # Collect consecutive non-Latin1 characters
        j = i
        while j < len(text) and ord(text[j]) >= 256:
            j += 1

        block = text[i:j]
        fixed = fix_block(block)
        if fixed is not None:
            result.append(fixed)
        else:
            result.append(block)
        i = j

    text1 = ''.join(result)

    # Pass 2: for lines that still have garbled CJK + '?' patterns,
    # try fixing the whole line
    lines = text1.split('\n')
    fixed_lines = []
    for line in lines:
        # Check if line has remaining garbled CJK mixed with '?'
        has_cjk = any(ord(c) >= 0x2000 for c in line)
        has_question = '?' in line
        if has_cjk and has_question:
            # Try whole-line fix (encode entire line to GBK, decode as UTF-8)
            # But only if line has a substantial CJK block
            cjk_chars = [c for c in line if ord(c) >= 0x2000]
            if len(cjk_chars) >= 3:
                fixed = fix_block(line)
                if fixed is not None:
                    fixed_lines.append(fixed)
                    continue
        fixed_lines.append(line)

    return '\n'.join(fixed_lines)


def fix_file(filepath, dry_run=True, report=None):
    """Fix a single file."""
    with open(filepath, 'rb') as f:
        raw = f.read()

    has_bom = raw[:3] == b'\xef\xbb\xbf'
    if has_bom:
        raw = raw[3:]

    try:
        text = raw.decode('utf-8')
    except UnicodeDecodeError:
        return []

    fixed = fix_text(text)

    if fixed == text:
        return []

    lines = text.split('\n')
    fixed_lines = fixed.split('\n')
    changes = []
    for i, (ol, fl) in enumerate(zip(lines, fixed_lines)):
        if ol != fl:
            changes.append((i + 1, ol, fl))

    if changes and report is not None:
        report.append(f"\n--- {filepath} ---")
        report.append(f"  Changed {len(changes)} lines")
        for ln, ol, fl in changes[:15]:
            report.append(f"  Line {ln}:")
            report.append(f"    OLD: {ol.strip()[:200]}")
            report.append(f"    NEW: {fl.strip()[:200]}")

    if not dry_run:
        output = fixed.encode('utf-8')
        if has_bom:
            output = b'\xef\xbb\xbf' + output
        with open(filepath, 'wb') as f:
            f.write(output)

    return changes


def main():
    if len(sys.argv) < 2:
        print("Usage: python _fix_encoding.py <directory_or_file> [--apply]")
        sys.exit(1)

    target = sys.argv[1]
    dry_run = '--apply' not in sys.argv

    if not dry_run:
        print("=" * 60)
        print("APPLY MODE - will modify files!")
        print("=" * 60)

    extensions = {'.vue', '.js', '.css', '.html', '.ts', '.json', '.txt', '.xml'}

    files_to_fix = []
    if os.path.isfile(target):
        files_to_fix = [target]
    else:
        for root, dirs, files in os.walk(target):
            dirs[:] = [d for d in dirs if d not in ('dist', 'node_modules', 'backup')]
            for fname in files:
                ext = os.path.splitext(fname)[1].lower()
                if ext in extensions:
                    files_to_fix.append(os.path.join(root, fname))

    report_lines = []
    total_files_changed = 0
    total_lines_changed = 0

    for fp in sorted(files_to_fix):
        changes = fix_file(fp, dry_run=dry_run, report=report_lines)
        if changes:
            total_files_changed += 1
            total_lines_changed += len(changes)

    report_path = '_encoding_fix_report.txt'
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write(f"Files checked: {len(files_to_fix)}\n")
        f.write(f"Files needing fix: {total_files_changed}\n")
        f.write(f"Total changed lines: {total_lines_changed}\n")
        f.write("=" * 60 + "\n")
        f.write('\n'.join(report_lines))

    print(f"Files checked: {len(files_to_fix)}")
    print(f"Files needing fix: {total_files_changed}")
    print(f"Total changed lines: {total_lines_changed}")
    print(f"Report: {report_path}")
    if dry_run:
        print("DRY RUN. Use --apply to fix.")


if __name__ == '__main__':
    main()
