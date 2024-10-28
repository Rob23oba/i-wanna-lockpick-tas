package iwltas;

import java.util.*;

public class IWLPhysicsSimulation {
	public float playerX; // objPlayer.x
	public float playerY; // objPlayer.y
	public float playerSpeed; // objPlayer.vspeed
	public boolean djump; // objPlayer.djump
	public List<Obstacle> obstacles = new ArrayList<>();

	public static final float jump = 8.5f; // -objPlayer.jump
	public static final float jump2 = 7f; // -objPlayer.jump2
	public static final float gravity = 0.4f; // objPlayer.gravity
	public static final float hgravity = 4.7699524e-9f; // rounding error lol

	public static final int LEFT = 1;
	public static final int RIGHT = 2;
	public static final int JUMP_PRESSED = 4;
	public static final int JUMP_RELEASED = 8;
	public static final int WALK = 16;
	public static final int RUN = 32;
	public static final int HOLD_MASK = LEFT | RIGHT | WALK | RUN;

	public static final int PLAYER_TOUCH_BLOCK = 1;
	public static final int PLAYER_TOUCH_GATE = 2;

	public int playerTouchMask(float x, float y) {
		int bboxLeft = (int) Math.rint(x - 5f);
		int bboxTop = (int) Math.rint(y - 12f);
		int bboxRight = bboxLeft + 11;
		int bboxBottom = bboxTop + 21;
		for (Obstacle o : obstacles) {
			if (o.bbx < bboxRight && o.bby < bboxBottom && bboxLeft < o.bbx + o.bbwidth && bboxTop < o.bby + o.bbheight) {
				return PLAYER_TOUCH_BLOCK;
			}
		}
		return 0;
	}

	public void moveContactSolid(float x, float y, float dx, float dy, int max) {
		while (max > 0) {
			float prevX = x;
			float prevY = y;
			x += dx;
			y += dy;
			if (playerTouchMask(x, y) != 0) {
				playerX = prevX;
				playerY = prevY;
				return;
			}
			max--;
		}
		playerX = x;
		playerY = y;
	}

	public void tick(int inputs) {
		// gml_Object_objPlayer_Step_0
		int onGroundMask = playerTouchMask(playerX, playerY + 1);
		int maxSpeed = 3;
		if ((inputs & WALK) != 0) {
			maxSpeed = 1;
		} else if ((inputs & RUN) == 0 && (onGroundMask & PLAYER_TOUCH_BLOCK) != 0) {
			// runswitch is on by default
			maxSpeed = 6;
		}
		int dir = 0;
		if ((inputs & RIGHT) != 0) {
			dir = 1;
		} else if ((inputs & LEFT) != 0) {
			dir = -1;
		}
		float hspeed = maxSpeed * dir + hgravity; // objPlayer.hspeed

		float vspeed = playerSpeed;

		// thanks gamemaker
		if (vspeed > 9.00001) {
			vspeed = 9;
		}
		if ((inputs & JUMP_PRESSED) != 0) {
			if (onGroundMask != 0) {
				vspeed = -jump;
				djump = true;
			} else if (djump) {
				vspeed = -jump2;
				djump = false;
			}
		}
		if ((inputs & JUMP_RELEASED) != 0) {
			// thanks again for being accurate
			if (vspeed < -0.00001) {
				vspeed = (float) (vspeed * 0.45);
			}
		}
		vspeed += gravity;

		float prevX = playerX; // objPlayer.xprevious
		float prevY = playerY; // objPlayer.yprevious
		playerX += hspeed;
		playerY += vspeed;

		int collision = playerTouchMask(playerX, playerY);
		if (collision != 0) {
			playerX = prevX;
			playerY = prevY;

			// gml_Object_objPlayer_Collision_28
			if (playerTouchMask(prevX + hspeed, prevY) != 0) {
				if (hspeed <= 0) {
					moveContactSolid(prevX, prevY, -1f, 8.742278e-8f, (int) -hspeed);
				} else {
					moveContactSolid(prevX, prevY, 1f, 0f, (int) hspeed);
				}
				hspeed = 0;
			}
			if (playerTouchMask(playerX, playerY + vspeed) != 0) {
				if (vspeed <= 0) {
					moveContactSolid(playerX, playerY, -4.371139e-8f, -1f, (int) -vspeed);
				} else {
					moveContactSolid(playerX, playerY, 1.1924881e-8f, 1f, (int) vspeed);
					djump = true;
				}
				vspeed = 0;
			}
			if (hspeed != 0 && playerTouchMask(playerX + hspeed, playerY + vspeed) != 0) {
				hspeed = 0;
			}
			playerX += hspeed;
			playerY += vspeed;
		}
		playerSpeed = vspeed;
	}
}
