"""La portada de los informes en GitHub Pages y los datos de las insignias del README.

Uso: python3 informes.py <carpeta>, con los artefactos del CI ya descargados dentro (backend-coverage,
frontend-coverage y lighthouse). Escribe <carpeta>/index.html y <carpeta>/badges/*.json en el formato "endpoint"
de shields.io: https://img.shields.io/endpoint?url=<dirección del json>
"""
import csv
import glob
import html
import json
import os
import sys

site = sys.argv[1]
os.makedirs(os.path.join(site, 'badges'), exist_ok=True)


def color(pct, good, ok):
    return 'brightgreen' if pct >= good else 'yellow' if pct >= ok else 'orange'


def badge(name, label, message, colour):
    with open(os.path.join(site, 'badges', f'{name}.json'), 'w', encoding='utf-8') as f:
        json.dump({'schemaVersion': 1, 'label': label, 'message': message, 'color': colour}, f)


rows = []

jacoco = os.path.join(site, 'backend-coverage', 'jacoco.csv')
if os.path.exists(jacoco):
    data = list(csv.DictReader(open(jacoco, encoding='utf-8')))
    covered = sum(int(r['LINE_COVERED']) for r in data)
    missed = sum(int(r['LINE_MISSED']) for r in data)
    pct = 100 * covered / (covered + missed)
    badge('backend-coverage', 'cobertura backend', f'{pct:.0f} %', color(pct, 80, 60))
    rows.append(('Cobertura del backend (JaCoCo)', f'{pct:.1f} % de las líneas', 'backend-coverage/index.html'))

vitest = os.path.join(site, 'frontend-coverage', 'coverage-summary.json')
if os.path.exists(vitest):
    pct = json.load(open(vitest, encoding='utf-8'))['total']['lines']['pct']
    badge('frontend-coverage', 'cobertura frontend', f'{pct:.0f} %', color(pct, 80, 50))
    rows.append(('Cobertura del frontend (Vitest)', f'{pct:.1f} % de las líneas', 'frontend-coverage/index.html'))

reports = sorted(glob.glob(os.path.join(site, 'lighthouse', 'lhr-*.json')))
if reports:
    scores = {}
    for path in reports:
        lhr = json.load(open(path, encoding='utf-8'))
        url = lhr['finalDisplayedUrl'].split('/', 3)[-1] or 'login'
        cats = {k: round(100 * v['score']) for k, v in lhr['categories'].items() if v['score'] is not None}
        scores[url] = cats
        page = os.path.relpath(path[:-5] + '.html', site).replace(os.sep, '/')
        rows.append((f'Lighthouse: /{url}',
                     ' · '.join(f'{k} {v}' for k, v in cats.items()), page))
    a11y = min(s.get('accessibility', 0) for s in scores.values())
    badge('lighthouse-accessibility', 'Lighthouse accesibilidad', str(a11y), color(a11y, 100, 90))

items = '\n'.join(
    f'<tr><th scope="row"><a href="{html.escape(link)}">{html.escape(name)}</a></th><td>{html.escape(value)}</td></tr>'
    for name, value, link in rows)
with open(os.path.join(site, 'index.html'), 'w', encoding='utf-8') as f:
    f.write(f"""<!doctype html>
<html lang="es">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<title>Informes · gespatiens</title>
<style>
  body {{ max-width: 760px; margin: 0 auto; padding: 32px 16px; font: 16px/1.5 system-ui, sans-serif;
         color: #161616; background: #fff; }}
  h1 {{ font-size: 28px; margin: 0 0 8px; }}
  table {{ width: 100%; border-collapse: collapse; margin-top: 24px; }}
  th, td {{ padding: 12px 8px; border-bottom: 1px solid #e0e0e0; text-align: left; vertical-align: top; }}
  th {{ font-weight: 600; }}
  a {{ color: #0043ce; }}
  @media (prefers-color-scheme: dark) {{
    body {{ color: #f4f4f4; background: #161616; }}
    th, td {{ border-color: #393939; }}
    a {{ color: #78a9ff; }}
  }}
</style>
</head>
<body>
<h1>Informes de gespatiens</h1>
<p>Del último CI en verde de la rama master. Código: <a href="https://github.com/EloyLagar/gespatiens-2026">EloyLagar/gespatiens-2026</a></p>
<table>
<thead><tr><th scope="col">Informe</th><th scope="col">Resultado</th></tr></thead>
<tbody>
{items}
</tbody>
</table>
</body>
</html>
""")
print('\n'.join(f'{n}: {v}' for n, v, _ in rows))
