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
package net.world.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;
import net.channel.ChannelWorldInterface;
import net.login.LoginWorldInterface;
import java.util.ArrayList;

/**
 *
 * @author Matze
 */
public interface WorldRegistry extends Remote {

    public void deregisterChannelServer(int channel, int world) throws RemoteException;

    public WorldLoginInterface registerLoginServer(String authKey, LoginWorldInterface cb) throws RemoteException;

    public void deregisterLoginServer(LoginWorldInterface cb) throws RemoteException;

    public WorldChannelInterface registerChannelServer(String key, ChannelWorldInterface cwi, int world) throws RemoteException;

    public void setProperty(String propertyName, Object value) throws RemoteException;

    public Object getProperty(String propertyName) throws RemoteException;

    public ArrayList<String> getPropertyNames() throws RemoteException;
}
