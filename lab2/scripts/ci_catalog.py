#!/usr/bin/env python3
"""Discover single-test GitHub Actions matrices directly from this repository."""
from __future__ import annotations

import argparse
import ast
import hashlib
import json
import os
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def item(stage: str, selector: str) -> dict[str, str]:
    digest = hashlib.sha256(f"{stage}:{selector}".encode()).hexdigest()[:12]
    prefix = {"unit_python": "up", "unit_android": "uk", "integration": "in", "e2e": "ee", "device": "ad"}[stage]
    return {"id": f"{prefix}-{digest}", "selector": selector, "title": selector.split("::")[-1].split(".")[-1]}


def python_cases(folder: str, stage: str) -> list[dict[str, str]]:
    cases = []
    for path in sorted((ROOT / "server" / "tests" / folder).glob("test_*.py")):
        tree = ast.parse(path.read_text(encoding="utf-8"), filename=str(path))
        for node in tree.body:
            if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)) and node.name.startswith("test_"):
                selector = f"tests/{folder}/{path.name}::{node.name}"
                cases.append(item(stage, selector))
            elif isinstance(node, ast.ClassDef) and node.name.startswith("Test"):
                for method in node.body:
                    if isinstance(method, (ast.FunctionDef, ast.AsyncFunctionDef)) and method.name.startswith("test_"):
                        selector = f"tests/{folder}/{path.name}::{node.name}::{method.name}"
                        cases.append(item(stage, selector))
    return cases


def kotlin_cases(directory: Path, stage: str, files_pattern: str) -> list[dict[str, str]]:
    cases = []
    pattern = re.compile(r"@Test\b(?:(?!@Test).)*?\bfun\s+(?:`([^`]+)`|([A-Za-z_][A-Za-z_0-9]*))\s*\(", re.S)
    for path in sorted(directory.glob(files_pattern)):
        source = path.read_text(encoding="utf-8")
        package = re.search(r"^package\s+([\w.]+)", source, re.M)
        klass = re.search(r"\bclass\s+(\w+Test)\b", source)
        if package is None or klass is None:
            raise ValueError(f"Could not identify test class in {path}")
        for match in pattern.finditer(source):
            method = match.group(1) or match.group(2)
            selector = f"{package.group(1)}.{klass.group(1)}.{method}"
            cases.append(item(stage, selector))
    return cases


def catalog() -> dict[str, list[dict[str, str]]]:
    data = ROOT / "mobile-app" / "data" / "src"
    result = {
        "unit_python": python_cases("unit", "unit_python"),
        "unit_android": kotlin_cases(data / "test" / "java" / "com" / "z23u184" / "studymate" / "data" / "lab", "unit_android", "CreateTaskUseCase*Test.kt"),
        "integration": python_cases("integration", "integration"),
        "e2e": python_cases("e2e", "e2e"),
        "device": kotlin_cases(data / "androidTest" / "java" / "com" / "z23u184" / "studymate" / "data", "device", "*Test.kt"),
    }
    for key, values in result.items():
        if not values:
            raise ValueError(f"No tests discovered in {key}; refusing a misleading green pipeline")
        if len({case["id"] for case in values}) != len(values):
            raise ValueError(f"Duplicate case IDs in {key}")
    return result


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--github-output", type=Path)
    parser.add_argument("--summary", action="store_true")
    args = parser.parse_args()
    cases = catalog()
    if args.github_output:
        with args.github_output.open("a", encoding="utf-8") as output:
            for name, items in cases.items():
                output.write(f"{name}={json.dumps(items, ensure_ascii=False, separators=(',', ':'))}\n")
    if args.summary:
        print(json.dumps({name: len(items) for name, items in cases.items()}, indent=2))
    elif not args.github_output:
        print(json.dumps(cases, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
