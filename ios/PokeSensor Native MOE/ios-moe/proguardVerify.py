#!/usr/bin/python2.7

import os
import re


re_import = re.compile(r'^import (apple\.[^\t]+[^.\t]Delegate);$')
re_proguard = re.compile(r'^-keep +interface (apple\.[^\t]+[^.\t]Delegate) ?\{.*}$')


def read_filter_imports(_file):
    import_set = set()

    with open(_file) as f:
        for line_number, line in enumerate(f, 1):
            l = line.strip()
            m = re_import.match(l)
            if m:
                d = m.group(1)
                print "Delegate found:", d, 'in file', _file, 'at line', line_number
                import_set.add(d)

    return import_set


def main():
    print "Start verify proguard rules for ***Delegate interfaces...\n"

    import_set = set()
    proguard_set = set()

    print "Parse source code..."
    for root, subFolders, files in os.walk("./src/"):
        for f in files:
            if f.endswith('.java'):
                import_set |= read_filter_imports(os.path.join(root, f))

    print "\nParse proguard.append.cfg..."
    with open("./proguard.append.cfg") as pf:
        for line_number, line in enumerate(pf, 1):
            l = line.strip()
            m = re_proguard.match(l)
            if m:
                d = m.group(1)
                print "Keep found:", d, 'at line', line_number
                proguard_set.add(d)

    missing_set = import_set.difference(proguard_set)

    if missing_set:
        print "\nGenerate missing rules into proguard.missing:"
        with open("proguard.missing", 'w') as missing_file:
            for d in missing_set:
                rule = 'proguard << \'-keep interface %s { *; }\\n\'' % d
                print rule
                missing_file.write(rule)
                missing_file.write('\n')
        exit(-1)
    else:
        print "\nNo missing rules!"


if __name__ == "__main__":
    main()
