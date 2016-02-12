/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package server.movement;

import java.awt.Point;
import tools.data.output.LittleEndianWriter;

/**
 *
 * @author Simon
 */
public class AranComboMovement extends AbstractLifeMovement {
//duration = unk

    public AranComboMovement(int type, Point position, int duration, int newstate) {
        super(type, position, duration, newstate);
    }

    @Override
    public void serialize(LittleEndianWriter lew) {
        lew.write(getType());
        lew.write(getNewstate());
        lew.writeShort(getDuration());
    }
}
