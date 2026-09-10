#!/usr/bin/env python3
"""根据 Git 历史生成确定性的核心组件 Markdown 记录。"""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from dataclasses import dataclass, field
from pathlib import Path


COMMIT_MARKER = "@@dsh-core-history@@"
FIELD_SEPARATOR = "\x1f"
SAFE_ID = re.compile(r"^[a-z0-9]+(?:-[a-z0-9]+)*$")


@dataclass
class Commit:
    sha: str
    author_date: str
    commit_date: str
    author: str
    subject: str
    paths: set[str] = field(default_factory=set)


def run_git(repo: Path, *args: str) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), *args],
        check=False,
        capture_output=True,
        text=True,
        encoding="utf-8",
        errors="replace",
    )
    if result.returncode:
        message = result.stderr.strip() or "Git 命令执行失败"
        raise RuntimeError(message)
    return result.stdout


def repository_root(start: Path) -> Path:
    return Path(run_git(start, "rev-parse", "--show-toplevel").strip()).resolve()


def load_units(config_path: Path) -> list[dict[str, object]]:
    try:
        data = json.loads(config_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise RuntimeError(f"无法读取配置 {config_path}: {error}") from error

    units = data.get("units") if isinstance(data, dict) else None
    if not isinstance(units, list) or not units:
        raise RuntimeError("配置必须包含非空的 'units' 数组")

    validated: list[dict[str, object]] = []
    seen: set[str] = set()
    for unit in units:
        if not isinstance(unit, dict):
            raise RuntimeError("每个核心组件必须是一个对象")
        unit_id = unit.get("id")
        name = unit.get("name")
        paths = unit.get("paths")
        if not isinstance(unit_id, str) or not SAFE_ID.fullmatch(unit_id):
            raise RuntimeError("每个组件 id 必须使用 kebab-case")
        if unit_id in seen:
            raise RuntimeError(f"组件 id 重复: {unit_id}")
        if not isinstance(name, str) or not name.strip():
            raise RuntimeError(f"组件 {unit_id} 必须提供非空名称")
        if not isinstance(paths, list) or not paths or not all(isinstance(path, str) and path for path in paths):
            raise RuntimeError(f"组件 {unit_id} 必须提供非空 paths 数组")
        seen.add(unit_id)
        validated.append({"id": unit_id, "name": name.strip(), "paths": paths})
    return validated


def parse_status_paths(line: str) -> list[str]:
    parts = line.split("\t")
    if len(parts) < 2 or not re.fullmatch(r"[A-Z][0-9]*", parts[0]):
        return []
    return [path for path in parts[1:] if path]


def collect_commits(repo: Path, paths: list[str]) -> list[Commit]:
    commits: dict[str, Commit] = {}
    format_string = (
        f"{COMMIT_MARKER}%H{FIELD_SEPARATOR}%aI{FIELD_SEPARATOR}%cI"
        f"{FIELD_SEPARATOR}%an{FIELD_SEPARATOR}%s"
    )
    for path in paths:
        output = run_git(
            repo,
            "log",
            "--follow",
            "--date=iso-strict",
            f"--format={format_string}",
            "--name-status",
            "--",
            path,
        )
        current: Commit | None = None
        for line in output.splitlines():
            if line.startswith(COMMIT_MARKER):
                fields = line[len(COMMIT_MARKER) :].split(FIELD_SEPARATOR, 4)
                if len(fields) != 5:
                    raise RuntimeError("Git 日志记录格式异常")
                parsed = Commit(*fields)
                current = commits.setdefault(parsed.sha, parsed)
                continue
            if current is not None:
                current.paths.update(parse_status_paths(line))
    return sorted(commits.values(), key=lambda commit: (commit.commit_date, commit.sha), reverse=True)


def markdown_escape(value: str) -> str:
    return value.replace("\\", "\\\\").replace("`", "\\`").replace("|", "\\|")


def relative_path(repo: Path, path: str) -> str:
    candidate = Path(path)
    if candidate.is_absolute():
        try:
            return candidate.resolve().relative_to(repo).as_posix()
        except ValueError:
            return candidate.as_posix()
    return candidate.as_posix()


def render(repo: Path, config_path: Path, units: list[dict[str, object]]) -> str:
    lines = [
        "# 核心组件变更历史",
        "",
        "本文件根据 Git 历史自动生成，是派生视图；Git 是唯一事实源。",
        "",
        f"配置文件：`{relative_path(repo, str(config_path))}`",
        "",
    ]
    for unit in units:
        unit_id = str(unit["id"])
        name = str(unit["name"])
        paths = [relative_path(repo, str(path)) for path in unit["paths"]]  # type: ignore[index]
        commits = collect_commits(repo, paths)
        lines.extend([f"## {markdown_escape(name)} (`{unit_id}`)", "", "跟踪路径："])
        lines.extend(f"- `{markdown_escape(path)}`" for path in paths)
        lines.append("")
        if not commits:
            lines.extend(["未找到相关提交。", ""])
            continue
        for commit in commits:
            short_sha = commit.sha[:12]
            lines.extend(
                [
                    f"### {commit.commit_date} | `{short_sha}` | {markdown_escape(commit.author)}",
                    "",
                    f"**提交：** {markdown_escape(commit.subject)}",
                    "",
                    "**变更路径：**",
                ]
            )
            changed = sorted(commit.paths) or paths
            lines.extend(f"- `{markdown_escape(path)}`" for path in changed)
            lines.append("")
    return "\n".join(lines).rstrip() + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--config", required=True, type=Path)
    parser.add_argument("--output", required=True, type=Path)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--check", action="store_true", help="输出文件不是最新状态时返回失败")
    args = parser.parse_args()

    try:
        repo = repository_root(args.repo.resolve())
        config_path = args.config.resolve()
        units = load_units(config_path)
        generated = render(repo, config_path, units)
        output_path = args.output.resolve()
        if args.check:
            current = output_path.read_text(encoding="utf-8") if output_path.exists() else None
            if current != generated:
                print(f"核心组件历史已过期: {output_path}", file=sys.stderr)
                return 1
            return 0
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(generated, encoding="utf-8", newline="\n")
        return 0
    except (OSError, RuntimeError) as error:
        print(f"生成核心组件历史失败: {error}", file=sys.stderr)
        return 2


if __name__ == "__main__":
    raise SystemExit(main())
