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
package net;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

public enum SendPacketOpcode implements WritableIntValueHolder {

    LOGIN_STATUS, //0x00
    SEND_LINK, //0x01
    SERVERSTATUS, //0x03
    GENDER_DONE, //0x04
    TOS, //0x05
    PIN_OPERATION, //0x06
    PIN_ASSIGNED, //0x07
    ALL_CHARLIST, //0x08
    SERVERLIST, //0x0A
    CHARLIST, //0x0B
    SERVER_IP, //0x0C
    CHAR_NAME_RESPONSE, //0x0D
    ADD_NEW_CHAR_ENTRY, //0x0E
    DELETE_CHAR_RESPONSE, //0x0F
    CHANGE_CHANNEL, //0x10
    PING, //0x11
    CHANNEL_SELECTED, //0x14
    RELOG_RESPONSE, //0x16
    MODIFY_INVENTORY_ITEM, //0x1A
    UPDATE_INVENTORY_SLOTS, //0x1B
    UPDATE_STATS, //0x1C
    GIVE_BUFF, //0x1D
    CANCEL_BUFF, //0x1E
    UPDATE_SKILLS, //0x21
    FAME_RESPONSE, //0x23
    SHOW_STATUS_INFO, //0x24
    SHOW_NOTES, //0x26
    TROCK_LOCATIONS, //0x27
    LIE_DETECTOR, //0x28
    REPORT_RESPONSE, //0x2A
    ENABLE_REPORT, //0x2C
    UPDATE_MOUNT, //0x2D
    SHOW_QUEST_COMPLETION, //0x2E
    SEND_TITLE_BOX, //0x2F
    USE_SKILL_BOOK, //0x30
    SHOW_EQUIP_EFFECT, //0x31
    FINISH_SORT, //0x32
    FINISH_SORT2, //0x33
    REPORTREPLY, //0x34
    MESO_LIMIT, //0x36
    GENDER, //0x37
    BBS_OPERATION, //0x38
    CHAR_INFO, //0x3A
    PARTY_OPERATION, //0x3B
    BUDDYLIST, //0x3C
    GUILD_OPERATION, //0x3E
    ALLIANCE_OPERATION, //0x3F
    SPAWN_PORTAL, //0x40
    SERVERMESSAGE, //0x41
    FAMILY_ACTION,//0x45
    YELLOW_TIP, //0x4A
    PLAYER_NPC, //0x4E
    MONSTERBOOK_ADD, //0x4F
    MONSTER_BOOK_CHANGE_COVER, //0x50
    ENERGY, //0x55
    SHOW_PEDIGREE, //0x57
    OPEN_FAMILY, //0x58
    FAMILY_MESSAGE, //0x59
    FAMILY_INVITE, //0x5A
    FAMILY_MESSAGE2, //0x5B
    FAMILY_SENIOR_MESSAGE, //0x5C
    FAMILY_GAIN_REP, //0x5E
    LOAD_FAMILY, //0x5D
    FAMILY_USE_REQUEST, //0x61
    CREATE_CYGNUS, //0x62
    BLANK_MESSAGE, //0x65
    AVATAR_MEGA, //0x67
    NAME_CHANGE_MESSAGE, //0x69
    UNKNOWN_MESSAGE, //0x6B
    GM_POLICE, //0x6C
    SILVER_BOX, //0x6D
    UNKNOWN_MESSAGE2, //0x6E
    SKILL_MACRO, //0x71
    WARP_TO_MAP, //0x72
    MTS_OPEN, //0x73
    CS_OPEN, //0x74
    RESET_SCREEN, //0x76
    CS_BLOCKED, //0x78
    FORCED_MAP_EQUIP, //0x79
    MULTICHAT, //0x7A
    WHISPER, //0x7B
    SPOUSE_CHAT, //0x7C
    BOSS_ENV, //0x7E
    BLOCK_PORTAL, //0x7F
    BLOCK_PORTAL_SHOP, //0x80
    MAP_EFFECT, //0x82
    GM_PACKET, //0x84
    OX_QUIZ, //0x85
    GMEVENT_INSTRUCTIONS, //0x86
    CLOCK, //0x87
    BOAT_EFFECT, //0x88
    STOP_CLOCK, //0x8E
    ARIANT_SCOREBOARD, //0x8F
    SPAWN_PLAYER, //0x91
    REMOVE_PLAYER_FROM_MAP, //0x92
    CHATTEXT, //0x93
    CHALKBOARD, //0x95
    UPDATE_CHAR_BOX, //0x96
    SHOW_SCROLL_EFFECT, //0x98
    SPAWN_PET, //0x99
    MOVE_PET, //0x9B
    PET_CHAT, //0x9C
    PET_NAMECHANGE, //0x9D
    PET_SHOW, //0x9E
    PET_COMMAND, //0x9F
    SPAWN_SPECIAL_MAPOBJECT, //0xA0
    REMOVE_SPECIAL_MAPOBJECT, //0xA1
    MOVE_SUMMON, //0xA2
    SUMMON_ATTACK, //0xA3
    DAMAGE_SUMMON, //0xA4
    SUMMON_SKILL, //0xA5
    MOVE_PLAYER, //0xA7
    CLOSE_RANGE_ATTACK, //0xA8
    RANGED_ATTACK, //0xA9
    MAGIC_ATTACK, //0xAA
    SKILL_EFFECT, //0xAC
    CANCEL_SKILL_EFFECT, //0xAD
    DAMAGE_PLAYER, //0xAE
    FACIAL_EXPRESSION, //0xAF
    SHOW_ITEM_EFFECT, //0xB1
    SHOW_CHAIR, //0xB2
    UPDATE_CHAR_LOOK, //0xB3
    SHOW_FOREIGN_EFFECT, //0xB4
    GIVE_FOREIGN_BUFF, //0xB5
    CANCEL_FOREIGN_BUFF, //0xB6
    UPDATE_PARTYMEMBER_HP, //0xB7
    CANCEL_CHAIR, //0xBB
    SHOW_ITEM_GAIN_INCHAT, //0xBC
    DOJO_WARP_UP, //0xBD
    LUCKSACK_PASS, //0xBE
    LUCKSACK_FAIL, //0xBF
    MESO_BAG_MESSAGE, //0xC0
    UPDATE_QUEST_INFO, //0xC1
    PLAYER_HINT, //0xC4
    KOREAN_EVENT, //0xC9
    CYGNUS_INTRO_LOCK, //0xCA
    CYGNUS_INTRO_DISABLE_UI, //0xCB
    CYGNUS_CHAR_CREATED, //0xCC
    COOLDOWN, //0xCE
    SPAWN_MONSTER, //0xD0
    KILL_MONSTER, //0xD1
    SPAWN_MONSTER_CONTROL, //0xD2
    MOVE_MONSTER, //0xD3
    MOVE_MONSTER_RESPONSE, //0xD4
    APPLY_MONSTER_STATUS, //0xD6
    CANCEL_MONSTER_STATUS, //0xD7
    DAMAGE_MONSTER, //0xDA
    ARIANT_THING, //0xDD
    SHOW_MONSTER_HP, //0xDE
    SHOW_DRAGGED, //0xDF
    SHOW_MAGNET, //0xE1
    CATCH_MONSTER, //0xE2
    SPAWN_NPC, //0xE3
    REMOVE_NPC, //0xE4
    SPAWN_NPC_REQUEST_CONTROLLER, //0xE5
    NPC_ACTION, //0xE6
    SPAWN_HIRED_MERCHANT, //0xEB
    DESTROY_HIRED_MERCHANT, //0xEC
    UPDATE_HIRED_MERCHANT, //0xED
    DROP_ITEM_FROM_MAPOBJECT, //0xEE
    REMOVE_ITEM_FROM_MAP, //0xEF
    KITE_MESSAGE, //0xF0
    KITE, //0xF1
    SPAWN_MIST, //0xF3
    REMOVE_MIST, //0xF4
    SPAWN_DOOR, //0xF5
    REMOVE_DOOR, //0xF6
    REACTOR_HIT, //0xF7
    REACTOR_SPAWN, //0xF9
    REACTOR_DESTROY, //0xFA
    ROLL_SNOWBALL, //0xFB
    HIT_SNOWBALL, //0xFC
    SNOWBALL_MESSAGE, //0xFD
    LEFT_KNOCK_BACK, //0xFE
    UNABLE_TO_CONNECT, //0x100
    MONSTER_CARNIVAL_START, //0x103
    MONSTER_CARNIVAL_OBTAINED_CP, //0x104
    MONSTER_CARNIVAL_PARTY_CP, //0x105
    MONSTER_CARNIVAL_SUMMON, //0x106
    MONSTER_CARNIVAL_DIED, //0x108
    ARIANT_PQ_START, //0x10B
    ZAKUM_SHRINE, //0x10D
    NPC_TALK, //0x10E
    OPEN_NPC_SHOP, //0x10F
    CONFIRM_SHOP_TRANSACTION, //0x110
    OPEN_STORAGE, //0x113
    MESSENGER, //0x117
    PLAYER_INTERACTION, //0x118
    DUEY, //0x120
    CS_UPDATE, //0x122
    CS_OPERATION, //0x123
    KEYMAP, //0x12A
    AUTO_HP_POT, //0x12B
    AUTO_MP_POT, //0x12C
    SEND_TV, //0x130
    REMOVE_TV, //0x131
    ENABLE_TV, //0x132
    MTS_OPERATION2, //0x136
    MTS_OPERATION, //0x137
    VICIOUS_HAMMER, //0x13D
    ENABLE_RECOMMENDED,
    SEND_RECOMMENDED,
    QUICK_SLOT,
    CATCH_ARIANT,
    CATCH_MOUNT,
    HPQ_MOON,
    ARIANT_SCORE,
    ARAN_COMBO,
    HIRED_MERCHANT_BOX; //0xE1
    private int code = -2;

    public void setValue(int code) {
        this.code = code;
    }

    @Override
    public int getValue() {
        return code;
    }

    public static Properties getDefaultProperties() throws FileNotFoundException, IOException {
        Properties props = new Properties();
        FileInputStream fileInputStream = new FileInputStream(System.getProperty("sendops"));
        props.load(fileInputStream);
        fileInputStream.close();
        return props;
    }

    static {
        try {
            ExternalCodeTableGetter.populateValues(getDefaultProperties(), values(), true);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load sendops", e);
        }
    }
}
