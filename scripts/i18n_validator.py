#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import os
import re

def get_keys_from_file(filepath):
    keys = set()
    if not os.path.exists(filepath):
        return keys
    with open(filepath, 'r', encoding='utf-8') as f:
        for line in f:
            line = line.strip()
            if line and not line.startswith('#') and '=' in line:
                key = line.split('=', 1)[0].strip()
                keys.add(key)
    return keys

def main():
    print("========================================")
    print("   BiblioHouse - Validador de i18n")
    print("========================================\n")
    
    # Rutas base
    src_dir = '../src/main/resources/com/ferlagod/bibliohousefx'
    if not os.path.exists(src_dir):
        # Intentar ejecutar desde el mismo directorio
        src_dir = 'src/main/resources/com/ferlagod/bibliohousefx'
        if not os.path.exists(src_dir):
            print("Error: No se encuentra la ruta de los properties.")
            return

    base_file = os.path.join(src_dir, 'messages.properties')
    if not os.path.exists(base_file):
        print(f"Error: No se encuentra el archivo base {base_file}")
        return

    base_keys = get_keys_from_file(base_file)
    print(f"✅ Encontradas {len(base_keys)} claves en el idioma base (ES).")

    locales = ['en', 'ca', 'gl', 'pt', 'eu']
    has_errors = False

    for loc in locales:
        loc_file = os.path.join(src_dir, f'messages_{loc}.properties')
        loc_keys = get_keys_from_file(loc_file)
        
        missing = base_keys - loc_keys
        extra = loc_keys - base_keys
        
        if missing or extra:
            has_errors = True
            print(f"\n❌ Errores en [ {loc.upper()} ] ({loc_file}):")
            if missing:
                print(f"  Faltan {len(missing)} claves:")
                for m in sorted(missing):
                    print(f"    - {m}")
            if extra:
                print(f"  Sobran {len(extra)} claves (no existen en base):")
                for e in sorted(extra):
                    print(f"    + {e}")
        else:
            print(f"✅ [ {loc.upper()} ] está sincronizado.")

    print("\n----------------------------------------")
    if has_errors:
        print("⚠️ Validación finalizada CON ERRORES. Por favor, revisa las traducciones.")
        exit(1)
    else:
        print("🎉 Validación finalizada SIN ERRORES. Todos los idiomas están al día.")
        exit(0)

if __name__ == "__main__":
    main()
