package org.anddev.andengine.engine.camera;

import javax.microedition.khronos.opengles.GL10;

import org.anddev.andengine.collision.CollisionChecker;
import org.anddev.andengine.engine.IUpdateHandler;
import org.anddev.andengine.engine.camera.hud.HUD;
import org.anddev.andengine.entity.primitive.RectangularShape;
import org.anddev.andengine.opengl.util.GLHelper;

import android.opengl.GLU;
import android.view.MotionEvent;

/**
 * @author Nicolas Gramlich
 * @since 10:24:18 - 25.03.2010
 */
public class Camera implements IUpdateHandler {
	// ===========================================================
	// Constants
	// ===========================================================

	// ===========================================================
	// Fields
	// ===========================================================

	private float mMinX;
	private float mMaxX;
	private float mMinY;
	private float mMaxY;

	private HUD mHUD;

	private boolean mFlipped;

	// ===========================================================
	// Constructors
	// ===========================================================

	public Camera(final float pX, final float pY, final float pWidth, final float pHeight) {
		this.mMinX = pX;
		this.mMaxX = pX + pWidth;
		this.mMinY = pY;
		this.mMaxY = pY + pHeight;
	}

	// ===========================================================
	// Getter & Setter
	// ===========================================================

	public float getMinX() {
		return this.mMinX;
	}

	public float getMaxX() {
		return this.mMaxX;
	}

	public float getMinY() {
		return this.mMinY;
	}

	public float getMaxY() {
		return this.mMaxY;
	}

	public float getWidth() {
		return (this.mMaxX - this.mMinX);
	}

	public float getHeight() {
		return (this.mMaxY - this.mMinY);
	}

	public float getCenterX() {
		return this.mMinX + (this.mMaxX - this.mMinX) * 0.5f;
	}

	public float getCenterY() {
		return this.mMinY + (this.mMaxY - this.mMinY) * 0.5f;
	}

	public void setCenter(final float pCenterX, final float pCenterY) {
		final float dX = pCenterX - this.getCenterX();
		final float dY = pCenterY - this.getCenterY();

		this.mMinX += dX;
		this.mMaxX += dX;
		this.mMinY += dY;
		this.mMaxY += dY;
	}

	public HUD getHUD() {
		return this.mHUD;
	}

	public void setHUD(final HUD pHUD) {
		this.mHUD = pHUD;
		pHUD.setCamera(this);
	}

	public boolean hasHUD() {
		return this.mHUD != null;
	}

	// ===========================================================
	// Methods for/from SuperClass/Interfaces
	// ===========================================================

	@Override
	public void onUpdate(final float pSecondsElapsed) {
		if(this.mHUD != null) {
			this.mHUD.onUpdate(pSecondsElapsed);
		}
	}
	
	@Override
	public void reset() {
		
	}

	// ===========================================================
	// Methods
	// ===========================================================

	public void flip() {
		this.mFlipped = !this.mFlipped;
	}

	public void onDrawHUD(final GL10 pGL) {
		if(this.mHUD != null) {
			this.mHUD.onDraw(pGL);
		}
	}

	public boolean isRectangularShapeVisible(final RectangularShape pRectangularShape) {
		final float otherLeft = pRectangularShape.getX();
		final float otherTop = pRectangularShape.getY();
		final float otherRight = pRectangularShape.getWidthScaled() + otherLeft;
		final float otherBottom = pRectangularShape.getHeightScaled() + otherTop;

		return CollisionChecker.checkAxisAlignedBoxCollision(this.getMinX(), this.getMinY(), this.getMaxX(), this.getMaxY(), otherLeft, otherTop, otherRight, otherBottom);
	}

	public void onApplyMatrix(final GL10 pGL) {
		GLHelper.setProjectionIdentityMatrix(pGL);

		GLU.gluOrtho2D(pGL, this.getMinX(), this.getMaxX(), this.getMaxY(), this.getMinY());

		if(this.mFlipped) {
			this.rotateHalfAround(pGL, this.getCenterX(), this.getCenterY());
		}
	}

	public void onApplyPositionIndependentMatrix(final GL10 pGL) {
		GLHelper.setProjectionIdentityMatrix(pGL);

		final float width = this.getWidth();
		final float height = this.getHeight();

		GLU.gluOrtho2D(pGL, 0, width, height, 0);

		if(this.mFlipped) {
			this.rotateHalfAround(pGL, width * 0.5f, height * 0.5f);
		}
	}

	private void rotateHalfAround(final GL10 pGL, final float pCenterX, final float pCenterY) {
		pGL.glTranslatef(pCenterX, pCenterY, 0);
		pGL.glRotatef(180, 0, 0, 1);
		pGL.glTranslatef(-pCenterX, -pCenterY, 0);
	}

	public void convertSceneToHUDMotionEvent(final MotionEvent pSceneMotionEvent) {
		final float x = pSceneMotionEvent.getX() - this.getMinX();
		final float y = pSceneMotionEvent.getY() - this.getMinY();
		pSceneMotionEvent.setLocation(x, y);
	}

	public void convertHUDToSceneMotionEvent(final MotionEvent pHUDMotionEvent) {
		final float x = pHUDMotionEvent.getX() + this.getMinX();
		final float y = pHUDMotionEvent.getY() + this.getMinY();
		pHUDMotionEvent.setLocation(x, y);
	}

	public void convertSurfaceToSceneMotionEvent(final MotionEvent pSurfaceMotionEvent, final int pSurfaceWidth, final int pSurfaceHeight) {
		final float relativeX;
		final float relativeY;

		if(this.mFlipped) {
			relativeX = 1 - (pSurfaceMotionEvent.getX() / pSurfaceWidth);
			relativeY = 1 - (pSurfaceMotionEvent.getY() / pSurfaceHeight);
		} else {
			relativeX = pSurfaceMotionEvent.getX() / pSurfaceWidth;
			relativeY = pSurfaceMotionEvent.getY() / pSurfaceHeight;
		}
		
		this.convertSurfaceToSceneMotionEvent(pSurfaceMotionEvent, relativeX, relativeY);
	}

	private void convertSurfaceToSceneMotionEvent(final MotionEvent pSurfaceMotionEvent, final float pRelativeX, final float pRelativeY) {
		final float minX = this.getMinX();
		final float maxX = this.getMaxX();
		final float minY = this.getMinY();
		final float maxY = this.getMaxY();
		
		final float x = minX + pRelativeX * (maxX - minX);
		final float y = minY + pRelativeY * (maxY - minY);

		pSurfaceMotionEvent.setLocation(x, y);
	}

	// ===========================================================
	// Inner and Anonymous Classes
	// ===========================================================
}
