"""Runs in the existing Python base image; no host Python installation needed."""
import base64
import os
from pathlib import Path
import re
import secrets
import sys
import tempfile

ROOT = Path(__file__).resolve().parent.parent
KEYS = ('DB_PASSWD', 'JWT_SECRET', 'NVD_ENCRYPTION_KEY')


def parse(text):
    values = {}
    for number, line in enumerate(text.splitlines(), 1):
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        match = re.fullmatch(r'([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)', line)
        if not match:
            raise ValueError(f'Linea {number}: usar NOMBRE=valor en una sola linea.')
        key, value = match.groups()
        if key in values:
            raise ValueError(f'Variable duplicada: {key}')
        if value.startswith(("'", '"')):
            quote = value[0]
            end = value.find(quote, 1)
            if end < 0 or (value[end + 1:].strip() and not value[end + 1:].strip().startswith('#')):
                raise ValueError(f'Formato de comillas no admitido: {key}')
            value = value[1:end]
        else:
            value = re.split(r'\s+#', value, maxsplit=1)[0].rstrip()
        # Keep interpretation identical to Compose; do not guess expansions/escapes.
        if '$' in value or '\\' in value:
            raise ValueError(f'{key}: no se admiten interpolaciones ni escapes en estos scripts.')
        values[key] = value
    return values


def main():
    path = ROOT / '.env'
    exists = path.exists()
    text = path.read_text(encoding='utf-8-sig') if exists else (ROOT / '.env.example').read_text(encoding='utf-8-sig')
    values = parse(text)
    missing = [key for key in KEYS if not values.get(key)]
    if sys.argv[1:] == ['existing-data'] and (missing or not exists):
        raise ValueError('Ya existe reconac_postgres_data. Restaurar .env y sus credenciales originales; no se regeneran claves.')
    if missing and (ROOT / 'recon_back' / '.env').exists():
        raise ValueError('Existe recon_back/.env. Migrar DB_USER, DB_PASSWD, JWT_SECRET y NVD_ENCRYPTION_KEY a .env raiz antes de iniciar; no se reemplazan secretos previos.')
    for key in missing:
        value = base64.b64encode(secrets.token_bytes(32)).decode('ascii')
        if key in values:
            text = re.sub(rf'(?m)^\s*{key}\s*=.*$', f'{key}={value}', text)
        else:
            text = text.rstrip() + f'\n{key}={value}\n'
        values[key] = value
    for key in ('DB_NAME', 'DB_USER'):
        if not re.fullmatch(r'[A-Za-z_][A-Za-z0-9_]*', values.get(key, '')):
            raise ValueError(f'{key}: requerido; usar letras, numeros y guion bajo.')
    for key in ('FRONTEND_PORT',):
        if not values.get(key, '').isdigit() or not 1 <= int(values[key]) <= 65535:
            raise ValueError(f'{key}: debe estar entre 1 y 65535.')
    if len(values['JWT_SECRET'].encode('utf-8')) < 32:
        raise ValueError('JWT_SECRET debe tener al menos 32 bytes UTF-8.')
    try:
        valid = len(base64.b64decode(values['NVD_ENCRYPTION_KEY'], validate=True)) == 32
    except ValueError:
        valid = False
    if not valid:
        raise ValueError('NVD_ENCRYPTION_KEY debe contener exactamente 32 bytes en Base64.')
    if not exists or missing:
        fd, temporary = tempfile.mkstemp(prefix='.env.', dir=ROOT)
        try:
            with os.fdopen(fd, 'w', encoding='utf-8', newline='\n') as stream:
                stream.write(text.rstrip() + '\n')
                stream.flush()
                os.fsync(stream.fileno())
            os.replace(temporary, path)
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)
        print('.env preparado. Guardarlo junto con los backups; no publicarlo.')
    else:
        print('.env validado; secretos conservados.')


if __name__ == '__main__':
    try:
        main()
    except (ValueError, OSError) as error:
        print(f'ERROR: {error}', file=sys.stderr)
        sys.exit(1)
