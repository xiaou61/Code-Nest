#!/usr/bin/env python3
"""
Build a dry-run report and optional SQL import for migrating You Deserve
Markdown questions into Code-Nest's xiaou-interview module.

The source directory is treated as read-only. The generated SQL is idempotent
for this migration source: it deletes previously imported You Deserve question
sets by a source marker, then inserts fresh categories, question sets, and
questions.
"""

from __future__ import annotations

import argparse
import json
import re
from collections import Counter, defaultdict
from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


SOURCE_MARKER = "source:you-deserve"
DEFAULT_SOURCE = r"F:\you-deserve"
DEFAULT_OUTPUT_DIR = r"output\you-deserve-migration"


@dataclass(frozen=True)
class Question:
    title: str
    slug: str
    category: str
    route: str
    difficulty: str
    scene: str
    order: int
    summary: str
    tags: list[str]
    body: str
    relative_path: str
    visual: dict[str, Any] | None = None

    @property
    def group_key(self) -> tuple[str, str]:
        return self.route, self.category


@dataclass
class QuestionSet:
    route: str
    category: str
    questions: list[Question] = field(default_factory=list)

    @property
    def title(self) -> str:
        if self.route == self.category:
            return self.category
        return f"{self.category}（{self.route}）"


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(
        description="Generate Code-Nest interview migration artifacts for F:\\you-deserve."
    )
    parser.add_argument("--source", default=DEFAULT_SOURCE, help="You Deserve project root.")
    parser.add_argument("--output-dir", default=DEFAULT_OUTPUT_DIR, help="Artifact output directory.")
    parser.add_argument(
        "--write-sql",
        action="store_true",
        help="Also generate you_deserve_interview_import.sql.",
    )
    parser.add_argument(
        "--no-visuals",
        action="store_true",
        help="Do not append question visual data into generated answers.",
    )
    parser.add_argument(
        "--creator-id",
        type=int,
        default=1,
        help="creator_id value used for imported official question sets.",
    )
    parser.add_argument(
        "--creator-name",
        default="You Deserve",
        help="creator_name value used for imported official question sets.",
    )
    parser.add_argument(
        "--sample-per-set",
        type=int,
        default=0,
        help="Keep only the first N ordered questions from each generated question set. Useful for test imports.",
    )
    parser.add_argument(
        "--apply-mysql",
        action="store_true",
        help="Apply the migration directly to a MySQL database with pymysql.",
    )
    parser.add_argument("--mysql-host", default="127.0.0.1")
    parser.add_argument("--mysql-port", type=int, default=3306)
    parser.add_argument("--mysql-user", default="root")
    parser.add_argument("--mysql-password", default="")
    parser.add_argument("--mysql-database", default="code_nest")
    return parser.parse_args()


def parse_frontmatter(raw: str) -> tuple[dict[str, Any], str]:
    if not raw.startswith("---"):
        return {}, raw

    match = re.match(r"^---\s*\n(.*?)\n---\s*\n?", raw, re.S)
    if not match:
        return {}, raw

    data: dict[str, Any] = {}
    lines = match.group(1).splitlines()
    index = 0

    while index < len(lines):
        line = lines[index]
        if not line.strip() or line.lstrip().startswith("#") or ":" not in line:
            index += 1
            continue

        key, value = line.split(":", 1)
        key = key.strip()
        value = value.strip()

        if value:
            data[key] = coerce_scalar(value)
            index += 1
            continue

        items: list[str] = []
        lookahead = index + 1
        while lookahead < len(lines) and re.match(r"^\s+-\s+", lines[lookahead]):
            items.append(re.sub(r"^\s+-\s+", "", lines[lookahead]).strip().strip("\"'"))
            lookahead += 1
        data[key] = items
        index = lookahead

    return data, raw[match.end() :]


def coerce_scalar(value: str) -> str | int:
    value = value.strip().strip("\"'")
    if re.fullmatch(r"-?\d+", value):
        return int(value)
    return value


def as_string(value: Any, fallback: str) -> str:
    return value if isinstance(value, str) and value.strip() else fallback


def as_int(value: Any, fallback: int) -> int:
    return value if isinstance(value, int) else fallback


def as_string_list(value: Any) -> list[str]:
    if not isinstance(value, list):
        return []
    return [item for item in value if isinstance(item, str) and item.strip()]


def load_visuals(source_root: Path) -> dict[str, dict[str, Any]]:
    visual_path = source_root / "content" / "visuals" / "question-visuals.json"
    if not visual_path.exists():
        return {}
    return json.loads(visual_path.read_text(encoding="utf-8"))


def load_questions(source_root: Path, include_visuals: bool) -> list[Question]:
    questions_root = source_root / "content" / "questions"
    if not questions_root.exists():
        raise FileNotFoundError(f"questions directory not found: {questions_root}")

    visuals = load_visuals(source_root) if include_visuals else {}
    questions: list[Question] = []

    for file_path in sorted(questions_root.rglob("*.md")) + sorted(questions_root.rglob("*.mdx")):
        raw = file_path.read_text(encoding="utf-8")
        frontmatter, body = parse_frontmatter(raw)
        fallback_slug = file_path.stem
        slug = as_string(frontmatter.get("slug"), fallback_slug)
        questions.append(
            Question(
                title=as_string(frontmatter.get("title"), fallback_slug),
                slug=slug,
                category=as_string(frontmatter.get("category"), "未分类"),
                route=as_string(frontmatter.get("route"), "通用路线"),
                difficulty=as_string(frontmatter.get("difficulty"), "medium"),
                scene=as_string(frontmatter.get("scene"), "面试常见"),
                order=as_int(frontmatter.get("order"), 999),
                summary=as_string(frontmatter.get("summary"), ""),
                tags=as_string_list(frontmatter.get("tags")),
                body=body.strip(),
                relative_path=file_path.relative_to(questions_root).as_posix(),
                visual=visuals.get(slug),
            )
        )

    return sorted(questions, key=lambda item: (item.route, item.category, item.order, item.title))


def build_question_sets(questions: list[Question]) -> list[QuestionSet]:
    grouped: dict[tuple[str, str], list[Question]] = defaultdict(list)
    for question in questions:
        grouped[question.group_key].append(question)

    sets: list[QuestionSet] = []
    for (route, category), items in grouped.items():
        sets.append(
            QuestionSet(
                route=route,
                category=category,
                questions=sorted(items, key=lambda item: (item.order, item.title)),
            )
        )

    return sorted(sets, key=lambda item: (-len(item.questions), item.route, item.category))


def sample_question_sets(question_sets: list[QuestionSet], sample_per_set: int) -> list[QuestionSet]:
    if sample_per_set <= 0:
        return question_sets

    sampled: list[QuestionSet] = []
    for question_set in question_sets:
        sampled.append(
            QuestionSet(
                route=question_set.route,
                category=question_set.category,
                questions=question_set.questions[:sample_per_set],
            )
        )
    return [question_set for question_set in sampled if question_set.questions]


def difficulty_label(value: str) -> str:
    return {
        "easy": "简单",
        "medium": "中等",
        "hard": "困难",
    }.get(value, value)


def render_answer(question: Question) -> str:
    lines = [
        f"<!-- {SOURCE_MARKER} slug:{question.slug} path:{question.relative_path} -->",
        "",
        "> 来源：You Deserve",
        f"> Slug：`{question.slug}`",
        f"> 路线：{question.route}",
        f"> 分类：{question.category}",
        f"> 难度：{difficulty_label(question.difficulty)}",
        f"> 场景：{question.scene}",
        f"> 标签：{', '.join(question.tags) if question.tags else '无'}",
    ]

    if question.summary:
        lines.append(f"> 摘要：{question.summary}")

    lines.extend(["", question.body.strip()])

    if question.visual:
        lines.extend(["", "## 图解数据", ""])
        visual = question.visual
        if visual.get("type"):
            lines.append(f"- 类型：{visual['type']}")
        if visual.get("summary"):
            lines.append(f"- 摘要：{visual['summary']}")
        nodes = visual.get("nodes")
        if isinstance(nodes, list) and nodes:
            lines.append("- 关键节点：")
            for index, node in enumerate(nodes, 1):
                if not isinstance(node, dict):
                    continue
                label = node.get("label", f"节点 {index}")
                detail = node.get("detail", "")
                lines.append(f"  {index}. {label}：{detail}")
        if visual.get("prompt"):
            lines.append(f"- 绘图提示：{visual['prompt']}")
        if visual.get("takeaway"):
            lines.append(f"- 记忆收口：{visual['takeaway']}")

    return "\n".join(lines).strip() + "\n"


def render_question_set_description(question_set: QuestionSet) -> str:
    difficulty_counts = Counter(question.difficulty for question in question_set.questions)
    difficulty_text = "，".join(
        f"{difficulty_label(name)} {count} 道" for name, count in difficulty_counts.most_common()
    )
    return "\n".join(
        [
            f"<!-- {SOURCE_MARKER} route:{question_set.route} category:{question_set.category} -->",
            "来自 You Deserve 题库的离线迁移内容。",
            f"路线：{question_set.route}",
            f"主题：{question_set.category}",
            f"题量：{len(question_set.questions)}",
            f"难度分布：{difficulty_text}",
        ]
    )


def sql_string(value: str | None) -> str:
    if value is None:
        return "NULL"
    cleaned = value.replace("\x00", "").replace("\\", "\\\\").replace("'", "''")
    return f"'{cleaned}'"


def chunked(items: list[Question], size: int) -> list[list[Question]]:
    return [items[index : index + size] for index in range(0, len(items), size)]


def build_sql(question_sets: list[QuestionSet], creator_id: int, creator_name: str) -> str:
    routes = sorted({question_set.route for question_set in question_sets})
    lines = [
        "-- Code-Nest You Deserve interview question migration",
        "-- Generated by scripts/you_deserve_to_interview_sql.py",
        "-- Review before applying to any database.",
        "START TRANSACTION;",
        "",
        f"DELETE iq FROM interview_question iq "
        f"JOIN interview_question_set iqs ON iq.question_set_id = iqs.id "
        f"WHERE iqs.description LIKE '%{SOURCE_MARKER}%';",
        f"DELETE FROM interview_question_set WHERE description LIKE '%{SOURCE_MARKER}%';",
        "",
        "-- Ensure route categories exist.",
    ]

    for sort_order, route in enumerate(routes, 1000):
        description = f"<!-- {SOURCE_MARKER} category-route:{route} -->\nYou Deserve 迁移路线：{route}"
        lines.append(
            "INSERT INTO interview_category "
            "(name, description, sort_order, question_set_count, status) VALUES "
            f"({sql_string(route)}, {sql_string(description)}, {sort_order}, 0, 1) "
            "ON DUPLICATE KEY UPDATE "
            "description = IF(description IS NULL OR description = '', VALUES(description), description), "
            "status = 1;"
        )

    lines.extend(["", "-- Insert question sets and questions."])

    for question_set in question_sets:
        lines.extend(
            [
                "",
                f"-- {question_set.title}",
                f"SET @category_id := (SELECT id FROM interview_category WHERE name = {sql_string(question_set.route)} LIMIT 1);",
                "INSERT INTO interview_question_set "
                "(title, description, category_id, type, visibility, question_count, view_count, favorite_count, status, "
                "creator_id, creator_name, create_time, update_time) VALUES "
                f"({sql_string(question_set.title)}, {sql_string(render_question_set_description(question_set))}, "
                f"@category_id, 1, 1, {len(question_set.questions)}, 0, 0, 1, "
                f"{creator_id}, {sql_string(creator_name)}, NOW(), NOW());",
                "SET @question_set_id := LAST_INSERT_ID();",
            ]
        )

        ordered_questions = list(enumerate(question_set.questions, 1))
        for batch_start in range(0, len(ordered_questions), 100):
            batch = ordered_questions[batch_start : batch_start + 100]
            values = []
            for actual_sort_order, question in batch:
                values.append(
                    "("
                    "@question_set_id, "
                    f"{sql_string(question.title)}, "
                    f"{sql_string(render_answer(question))}, "
                    f"{actual_sort_order}, 0, 0, NOW(), NOW()"
                    ")"
                )
            lines.append(
                "INSERT INTO interview_question "
                "(question_set_id, title, answer, sort_order, view_count, favorite_count, create_time, update_time) VALUES\n"
                + ",\n".join(values)
                + ";"
            )

    route_list = ", ".join(sql_string(route) for route in routes)
    lines.extend(
        [
            "",
            "-- Refresh counters for affected route categories.",
            "UPDATE interview_question_set iqs",
            "SET question_count = (SELECT COUNT(*) FROM interview_question iq WHERE iq.question_set_id = iqs.id)",
            f"WHERE iqs.description LIKE '%{SOURCE_MARKER}%';",
            "",
            "UPDATE interview_category ic",
            "SET question_set_count = (SELECT COUNT(*) FROM interview_question_set iqs WHERE iqs.category_id = ic.id)",
            f"WHERE ic.name IN ({route_list});",
            "",
            "COMMIT;",
            "",
        ]
    )
    return "\n".join(lines)


def apply_mysql(
    question_sets: list[QuestionSet],
    *,
    host: str,
    port: int,
    user: str,
    password: str,
    database: str,
    creator_id: int,
    creator_name: str,
) -> dict[str, int]:
    try:
        import pymysql
    except ImportError as exc:
        raise RuntimeError("pymysql is required for --apply-mysql") from exc

    routes = sorted({question_set.route for question_set in question_sets})
    connection = pymysql.connect(
        host=host,
        port=port,
        user=user,
        password=password,
        database=database,
        charset="utf8mb4",
        autocommit=False,
    )

    inserted_sets = 0
    inserted_questions = 0
    deleted_sets = 0

    try:
        with connection.cursor() as cursor:
            cursor.execute(
                """
                DELETE iq FROM interview_question iq
                JOIN interview_question_set iqs ON iq.question_set_id = iqs.id
                WHERE iqs.description LIKE %s
                """,
                (f"%{SOURCE_MARKER}%",),
            )
            deleted_sets = cursor.execute(
                "DELETE FROM interview_question_set WHERE description LIKE %s",
                (f"%{SOURCE_MARKER}%",),
            )

            for sort_order, route in enumerate(routes, 1000):
                description = (
                    f"<!-- {SOURCE_MARKER} category-route:{route} -->\n"
                    f"You Deserve 迁移路线：{route}"
                )
                cursor.execute(
                    """
                    INSERT INTO interview_category
                        (name, description, sort_order, question_set_count, status)
                    VALUES (%s, %s, %s, 0, 1)
                    ON DUPLICATE KEY UPDATE
                        description = IF(description IS NULL OR description = '', VALUES(description), description),
                        status = 1
                    """,
                    (route, description, sort_order),
                )

            category_ids: dict[str, int] = {}
            for route in routes:
                cursor.execute("SELECT id FROM interview_category WHERE name = %s LIMIT 1", (route,))
                row = cursor.fetchone()
                if row is None:
                    raise RuntimeError(f"category not found after insert: {route}")
                category_ids[route] = int(row[0])

            for question_set in question_sets:
                cursor.execute(
                    """
                    INSERT INTO interview_question_set
                        (title, description, category_id, type, visibility, question_count,
                         view_count, favorite_count, status, creator_id, creator_name, create_time, update_time)
                    VALUES (%s, %s, %s, 1, 1, %s, 0, 0, 1, %s, %s, NOW(), NOW())
                    """,
                    (
                        question_set.title,
                        render_question_set_description(question_set),
                        category_ids[question_set.route],
                        len(question_set.questions),
                        creator_id,
                        creator_name,
                    ),
                )
                question_set_id = int(cursor.lastrowid)
                inserted_sets += 1

                rows = [
                    (
                        question_set_id,
                        question.title,
                        render_answer(question),
                        sort_order,
                        0,
                        0,
                    )
                    for sort_order, question in enumerate(question_set.questions, 1)
                ]
                if rows:
                    cursor.executemany(
                        """
                        INSERT INTO interview_question
                            (question_set_id, title, answer, sort_order, view_count, favorite_count, create_time, update_time)
                        VALUES (%s, %s, %s, %s, %s, %s, NOW(), NOW())
                        """,
                        rows,
                    )
                    inserted_questions += len(rows)

            cursor.execute(
                """
                UPDATE interview_question_set iqs
                SET question_count = (
                    SELECT COUNT(*) FROM interview_question iq WHERE iq.question_set_id = iqs.id
                )
                WHERE iqs.description LIKE %s
                """,
                (f"%{SOURCE_MARKER}%",),
            )
            if routes:
                cursor.execute(
                    """
                    UPDATE interview_category ic
                    SET question_set_count = (
                        SELECT COUNT(*) FROM interview_question_set iqs WHERE iqs.category_id = ic.id
                    )
                    WHERE ic.name IN ({})
                    """.format(",".join(["%s"] * len(routes))),
                    tuple(routes),
                )

        connection.commit()
    except Exception:
        connection.rollback()
        raise
    finally:
        connection.close()

    return {
        "deleted_question_sets": deleted_sets,
        "inserted_question_sets": inserted_sets,
        "inserted_questions": inserted_questions,
        "touched_categories": len(routes),
    }


def build_summary(questions: list[Question], question_sets: list[QuestionSet]) -> dict[str, Any]:
    slug_map: dict[str, list[str]] = defaultdict(list)
    title_map: dict[str, list[str]] = defaultdict(list)
    for question in questions:
        slug_map[question.slug].append(question.relative_path)
        title_map[question.title].append(question.relative_path)

    body_lengths = sorted(len(question.body) for question in questions)
    answer_bytes = sorted(len(render_answer(question).encode("utf-8")) for question in questions)
    missing_visuals = [question.slug for question in questions if question.visual is None]

    return {
        "question_count": len(questions),
        "question_set_count": len(question_sets),
        "route_count": len({question.route for question in questions}),
        "source_category_count": len({question.category for question in questions}),
        "routes": Counter(question.route for question in questions).most_common(),
        "source_categories": Counter(question.category for question in questions).most_common(),
        "route_category_sets": [
            {
                "route": question_set.route,
                "category": question_set.category,
                "title": question_set.title,
                "question_count": len(question_set.questions),
            }
            for question_set in question_sets
        ],
        "difficulty": Counter(question.difficulty for question in questions).most_common(),
        "duplicate_slug_count": sum(1 for paths in slug_map.values() if len(paths) > 1),
        "duplicate_title_count": sum(1 for paths in title_map.values() if len(paths) > 1),
        "duplicate_slug_examples": {slug: paths for slug, paths in slug_map.items() if len(paths) > 1},
        "duplicate_title_examples": {title: paths for title, paths in title_map.items() if len(paths) > 1},
        "missing_visual_count": len(missing_visuals),
        "missing_visual_examples": missing_visuals[:20],
        "body_length": {
            "min": body_lengths[0] if body_lengths else 0,
            "p50": body_lengths[len(body_lengths) // 2] if body_lengths else 0,
            "p95": body_lengths[int(len(body_lengths) * 0.95)] if body_lengths else 0,
            "max": body_lengths[-1] if body_lengths else 0,
        },
        "answer_bytes": {
            "min": answer_bytes[0] if answer_bytes else 0,
            "p50": answer_bytes[len(answer_bytes) // 2] if answer_bytes else 0,
            "p95": answer_bytes[int(len(answer_bytes) * 0.95)] if answer_bytes else 0,
            "max": answer_bytes[-1] if answer_bytes else 0,
            "over_mysql_text_limit": sum(1 for size in answer_bytes if size > 65535),
        },
    }


def write_markdown_summary(output_path: Path, summary: dict[str, Any], sql_written: bool) -> None:
    lines = [
        "# You Deserve Interview Migration Dry Run",
        "",
        f"- Questions: {summary['question_count']}",
        f"- Question sets: {summary['question_set_count']}",
        f"- Route categories: {summary['route_count']}",
        f"- Source categories: {summary['source_category_count']}",
        f"- Duplicate slugs: {summary['duplicate_slug_count']}",
        f"- Duplicate titles: {summary['duplicate_title_count']}",
        f"- Missing visuals: {summary['missing_visual_count']}",
        f"- Answer bytes over MySQL TEXT limit: {summary['answer_bytes']['over_mysql_text_limit']}",
        f"- SQL generated: {'yes' if sql_written else 'no'}",
        "",
        "## Difficulty",
        "",
    ]

    for difficulty, count in summary["difficulty"]:
        lines.append(f"- {difficulty_label(difficulty)}: {count}")

    lines.extend(["", "## Route Categories", ""])
    for route, count in summary["routes"]:
        lines.append(f"- {route}: {count}")

    lines.extend(["", "## Question Sets", ""])
    for item in summary["route_category_sets"]:
        lines.append(f"- {item['title']}: {item['question_count']} questions")

    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    args = parse_args()
    source_root = Path(args.source)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    questions = load_questions(source_root, include_visuals=not args.no_visuals)
    question_sets = sample_question_sets(build_question_sets(questions), args.sample_per_set)
    if args.sample_per_set > 0:
        questions = [question for question_set in question_sets for question in question_set.questions]
    summary = build_summary(questions, question_sets)

    manifest = {
        "source": str(source_root),
        "source_marker": SOURCE_MARKER,
        "strategy": {
            "target_module": "xiaou-interview",
            "category": "source route -> interview_category.name",
            "question_set": "(source route, source category) -> interview_question_set",
            "question": "one source Markdown file -> one interview_question",
            "answer": "source metadata + original Markdown body + optional visual data",
            "sample_per_set": args.sample_per_set,
        },
        "summary": summary,
    }
    (output_dir / "you_deserve_interview_manifest.json").write_text(
        json.dumps(manifest, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )

    sql_written = False
    if args.write_sql:
        sql = build_sql(question_sets, creator_id=args.creator_id, creator_name=args.creator_name)
        (output_dir / "you_deserve_interview_import.sql").write_text(sql, encoding="utf-8")
        sql_written = True

    apply_result = None
    if args.apply_mysql:
        apply_result = apply_mysql(
            question_sets,
            host=args.mysql_host,
            port=args.mysql_port,
            user=args.mysql_user,
            password=args.mysql_password,
            database=args.mysql_database,
            creator_id=args.creator_id,
            creator_name=args.creator_name,
        )
        manifest["mysql_apply_result"] = apply_result
        (output_dir / "you_deserve_interview_manifest.json").write_text(
            json.dumps(manifest, ensure_ascii=False, indent=2),
            encoding="utf-8",
        )

    write_markdown_summary(output_dir / "you_deserve_interview_summary.md", summary, sql_written)

    print(json.dumps({"summary": summary, "mysql_apply_result": apply_result}, ensure_ascii=False, indent=2))


if __name__ == "__main__":
    main()
