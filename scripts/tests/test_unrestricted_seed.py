import copy
import json
from pathlib import Path
import runpy
import tempfile
import unittest

m = runpy.run_path(str(Path(__file__).resolve().parents[1] / 'seed_unrestricted_places.py'))


class UnrestrictedSeedTest(unittest.TestCase):
    def test_reviewed_notion_rows_have_no_blank_and_distinct_origins(self):
        data = m['load_fixture'](m['DEFAULT_FILE'])
        self.assertEqual(9, len(data['places']))
        self.assertEqual(5, sum(p['category'] == 'CAFE' for p in data['places']))
        self.assertEqual(9, len({p['businessNumber'] for p in data['places']}))

    def test_a_restricted_or_unverified_coordinate_fixture_is_rejected(self):
        data = json.loads(m['DEFAULT_FILE'].read_text())
        for key, value in [('originalCondition', '7kg 미만'), ('latitude', 0)]:
            changed = copy.deepcopy(data); changed['places'][0][key] = value
            with tempfile.TemporaryDirectory() as folder:
                p = Path(folder) / 'fixture.json'; p.write_text(json.dumps(changed))
                with self.assertRaises(ValueError): m['load_fixture'](p)

    def test_no_indoor_or_outdoor_constraint_can_make_a_dog_unknown_or_denied(self):
        c = m['unrestricted_conditions']()
        self.assertTrue(c['petsAllowed'])
        for key in ('indoor', 'outdoor'):
            self.assertTrue(c[key]['allowed'])
            self.assertIsNone(c[key]['maxWeightKg'])
            self.assertEqual([], c[key]['equipmentCodes'])
        self.assertFalse(c['vaccinationConfirmed'])
