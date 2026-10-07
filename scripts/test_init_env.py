import base64
import contextlib
import importlib.util
import io
from pathlib import Path
import sys
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location('init_env', Path(__file__).with_name('init-env.py'))
INIT = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(INIT)
TEMPLATE = Path(__file__).resolve().parent.parent.joinpath('.env.example').read_text(encoding='utf-8')


class InitEnvTests(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.addCleanup(self.tmp.cleanup)
        self.root = Path(self.tmp.name)
        self.env = self.root / '.env'
        (self.root / '.env.example').write_text(TEMPLATE, encoding='utf-8')

    def run_init(self, existing=False):
        with patch.object(INIT, 'ROOT', self.root), patch.object(sys, 'argv', ['init-env.py', 'existing-data' if existing else 'new-data']), contextlib.redirect_stdout(io.StringIO()):
            INIT.main()

    def test_generated_keys_and_repeated_start_preserve_bytes(self):
        self.run_init()
        before = self.env.read_bytes()
        values = INIT.parse(before.decode('utf-8'))
        for key in INIT.KEYS:
            self.assertEqual(len(base64.b64decode(values[key], validate=True)), 32)
        self.assertEqual(len({values[key] for key in INIT.KEYS}), 3)
        self.run_init(existing=True)
        self.assertEqual(self.env.read_bytes(), before)

    def test_existing_volume_without_env_never_creates_keys(self):
        with self.assertRaises(ValueError):
            self.run_init(existing=True)
        self.assertFalse(self.env.exists())

    def test_existing_volume_with_missing_key_does_not_write(self):
        self.env.write_text(TEMPLATE, encoding='utf-8')
        before = self.env.read_bytes()
        with self.assertRaises(ValueError):
            self.run_init(existing=True)
        self.assertEqual(self.env.read_bytes(), before)

    def test_native_configuration_requires_migration(self):
        (self.root / 'recon_back').mkdir()
        (self.root / 'recon_back/.env').write_text('JWT_SECRET=previous', encoding='utf-8')
        with self.assertRaises(ValueError):
            self.run_init()
        self.assertFalse(self.env.exists())

    def test_invalid_existing_keys_are_not_replaced(self):
        self.run_init()
        text = self.env.read_text(encoding='utf-8')
        values = INIT.parse(text)
        for key in ('JWT_SECRET', 'NVD_ENCRYPTION_KEY'):
            with self.subTest(key=key):
                self.env.write_text(text.replace(values[key], 'invalid'), encoding='utf-8')
                before = self.env.read_bytes()
                with self.assertRaises(ValueError):
                    self.run_init()
                self.assertEqual(self.env.read_bytes(), before)

    def test_parser_rejects_ambiguity(self):
        for text in ('X=1\nX=2', 'X=${OTHER}', 'X="unterminated'):
            with self.subTest(text=text), self.assertRaises(ValueError):
                INIT.parse(text)
        self.assertEqual(INIT.parse("X='hello world' # comment\nY=plain # comment"), {'X': 'hello world', 'Y': 'plain'})


if __name__ == '__main__':
    unittest.main()
