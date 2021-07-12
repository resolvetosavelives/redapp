#!/usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import json

env_file = open(sys.argv[1])
env_json = json.loads(env_file.read())
env_file.close()

server_config = env_json['env']
server_vars = list(filter(lambda item: 'value' in item[1], server_config.items()))

server_vars_map = dict(map(lambda item: (item[0], item[1]['value']), server_vars))
print(json.dumps(server_vars_map, indent=4))