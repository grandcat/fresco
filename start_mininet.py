#!/usr/bin/env python
# -*- coding: utf-8 -*-
import argparse
import time
from subprocess import call

from mininet.topo import Topo
from mininet.node import OVSController
from mininet.link import TCLink
from mininet.net import Mininet
from mininet.log import setLogLevel

# Simulation parameters and configuration
DEF_NUM_PARTIES = 5

LINK_CONFIG = {
    # Options: bw=10, delay='5ms', loss=10, max_queue_size=1000, use_htb=True
    'delay': '1.0ms',
    'loss': 0,
    'use_htb': True
    }


class SingleSwitchTopo(Topo):
    """Single switch connected to n hosts."""
    def build(self, n=3):
        switch = self.addSwitch('s1')
        for h in range(n):
            host = self.addHost('h%s' % (h + 1))
            # Configure links with delay and packet loss
            self.addLink(host, switch, **LINK_CONFIG)


def generate_node_config(node_id, hosts):
    """Node ID and IPs of all members for URI parser"""
    config_str = "-i{0} ".format(node_id)

    for identifier, h in enumerate(hosts):
        config_str += "-p{0}:{1}:9001 ".format(str(identifier + 1), h.IP())

    return config_str


def run(num_nodes):
    flat_topo = SingleSwitchTopo(n=num_nodes)
    net = Mininet(topo=flat_topo, link=TCLink)

    net.start()

    # print("Testing network connectivity")
    # net.pingAll()

    # Get host configurations
    hosts = [net.getNodeByName(h) for h in flat_topo.hosts()]
    print(hosts)

    print("Start Fresco nodes")

    for i, host in enumerate(hosts):
        # time.sleep(0.1)

        # Best evaluator so far: -eSEQUENTIAL_BATCHED (ok: -ePARALLEL_BATCHED)
        # Needs -hold for xterm
        # xterm -hold -geometry 130x40+0+900 -e
        host.cmd('xterm -hold -geometry 130x40+0+900 -e java -cp target/fresco-0.2-SNAPSHOT-jar-with-dependencies.jar dk.alexandra.fresco.demo.DistSum -sbgw -ePARALLEL %s -tid "virt_eP_l1ms" &' % (generate_node_config(i + 1, hosts)))
        #print(host.readline())

    raw_input('Press enter to stop all nodes.')

    print("Try killing xterms")
    call(["killall", "xterm"])

    net.stop()

if __name__ == '__main__':
    parser = argparse.ArgumentParser(description='Runs multiple virtual nodes using Mininet')
    parser.add_argument('-N', nargs='?', help='Number of nodes',
                        type=int, default=DEF_NUM_PARTIES, choices=range(2, 30, 1))
    args = parser.parse_args()

    # Tell mininet to print useful information
    setLogLevel('info')
    run(args.N)
