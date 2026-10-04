"""Plan discovery is version-aware: the highest v<N>/ that carries a plan is current (numeric order), a module without one keeps the shallowest match."""
import tempfile
import unittest
from pathlib import Path
from unittest import mock

from tests import _paths  # noqa: F401  (puts the generator root on sys.path)
import discovery


def _touch(root: Path, rel: str) -> Path:
    path = root / rel
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text("**API REGISTRY**\n", encoding="utf-8")
    return path


class ExecutionPlanDiscovery(unittest.TestCase):

    def setUp(self):
        self._tmp = tempfile.TemporaryDirectory()
        self.module_dir = Path(self._tmp.name) / "FX"
        self.module_dir.mkdir()
        patcher = mock.patch.object(discovery, "default_module_dir", return_value=self.module_dir)
        patcher.start()
        self.addCleanup(patcher.stop)
        self.addCleanup(self._tmp.cleanup)

    def test_incremental_version_plan_wins_over_v1(self):
        _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        v2 = _touch(self.module_dir, "v2/P3_1/backend-execution-plan-fx.md")
        self.assertEqual(discovery.find_execution_plan("FX"), v2)

    def test_version_order_is_numeric_not_lexical(self):
        _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        _touch(self.module_dir, "v2/P3_1/backend-execution-plan-fx.md")
        v10 = _touch(self.module_dir, "v10/P3_1/backend-execution-plan-fx.md")
        self.assertEqual(discovery.find_execution_plan("FX"), v10)

    def test_no_version_folder_keeps_shallowest_match(self):
        plan = _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        _touch(self.module_dir, "packages/backend-execution/SVC-API/backend-execution-plan-fx.md")
        self.assertEqual(discovery.find_execution_plan("FX"), plan)

    def test_version_folder_without_a_plan_yet_keeps_previous_plan(self):
        plan = _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        (self.module_dir / "v2" / "P3_1").mkdir(parents=True)
        self.assertEqual(discovery.find_execution_plan("FX"), plan)

    def test_shallowest_rule_applies_inside_the_chosen_version(self):
        _touch(self.module_dir, "P3_1/backend-execution-plan-fx.md")
        v2 = _touch(self.module_dir, "v2/P3_1/backend-execution-plan-fx.md")
        _touch(self.module_dir, "v2/packages/backend-execution/SVC-API/backend-execution-plan-fx.md")
        self.assertEqual(discovery.find_execution_plan("FX"), v2)

    def test_no_plan_anywhere_is_none(self):
        self.assertIsNone(discovery.find_execution_plan("FX"))


if __name__ == "__main__":
    unittest.main()
