/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc>
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as
published by the Free Software Foundation version 3 as published by
the Free Software Foundation. You may not use, modify or distribute
this program under any other version of the GNU Affero General Public
License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.channel;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Collections;
import client.MapleCharacter;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PlayerStorage implements IPlayerStorage {

    Map<String, MapleCharacter> nameToChar = new LinkedHashMap<String, MapleCharacter>();
    Map<Integer, MapleCharacter> idToChar = new LinkedHashMap<Integer, MapleCharacter>();
    public ReentrantReadWriteLock storageLock = new ReentrantReadWriteLock();

    public void registerPlayer(MapleCharacter chr) {
        storageLock.writeLock().lock();
        try {
            nameToChar.put(chr.getName().toLowerCase(), chr);
            idToChar.put(chr.getId(), chr);
        } finally {
            storageLock.writeLock().unlock();
        }
    }

    public void deregisterPlayer(MapleCharacter chr) {
        storageLock.writeLock().lock();
        try {
            nameToChar.remove(chr.getName().toLowerCase());
            idToChar.remove(chr.getId());
        } finally {
            storageLock.writeLock().unlock();
        }
    }

    public MapleCharacter getCharacterByName(String name) {
        storageLock.readLock().lock();
        try {
            return nameToChar.get(name.toLowerCase());
        } finally {
            storageLock.readLock().unlock();
        }
    }

    public MapleCharacter getCharacterById(int id) {
        storageLock.readLock().lock();
        try {
            return idToChar.get(Integer.valueOf(id));
        } finally {
            storageLock.readLock().unlock();
        }
    }

    public Collection<MapleCharacter> getAllCharacters() {
        return Collections.unmodifiableCollection(nameToChar.values());
    }
}
