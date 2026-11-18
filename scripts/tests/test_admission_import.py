import runpy
import unittest
from pathlib import Path

module = runpy.run_path(str(Path(__file__).resolve().parents[1] / 'import_admission_evidence.py'))


class AdmissionImportTest(unittest.TestCase):
    def test_list_membership_or_failed_detail_is_not_admission_evidence(self):
        confirm = module['confirmed_kto']
        self.assertFalse(confirm({'items': []}))
        self.assertFalse(confirm({'error': '429'}))
        self.assertFalse(confirm({'items': [{'acmpyTypeCd': '미확인'}]}))
        self.assertTrue(confirm({'items': [{'acmpyTypeCd': '일부구역 동반가능'}]}))

    def test_missing_conditions_are_not_invented(self):
        policy = module['team_policy']({'준비물': '["리드줄","캐리어"]', '예방접종': '["필수"]'})
        self.assertEqual([], policy['equipment'])
        self.assertIsNone(policy['max_weight_kg'])

    def test_strict_weight_and_unambiguous_equipment(self):
        policy = module['team_policy']({'크기 제한': '["있음","7kg 미만"]', '준비물': '["캐리어"]'})
        self.assertEqual(7, policy['max_weight_kg'])
        self.assertTrue(policy['max_weight_exclusive'])
        self.assertEqual(['CARRIER'], policy['equipment'])
