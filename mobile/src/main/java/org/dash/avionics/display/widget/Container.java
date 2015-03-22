package org.dash.avionics.display.widget;

import android.graphics.Canvas;

import com.google.common.collect.Lists;

import java.util.List;

public class Container extends Widget {

  protected final List<Widget> mChildren = Lists.newArrayList();

  @Override
  protected void drawContents(Canvas canvas) {
    for (Widget w : mChildren) {
      w.draw(canvas);
    }
  }

  protected void center(Widget w) {
    w.moveTo(
        (getWidth() - w.getWidth()) / 2f,
        (getHeight() - w.getHeight()) / 2f);
  }

  protected void centerX(Widget w) {
    w.moveTo(
        (getWidth() - w.getWidth()) / 2f,
        w.getY());
  }

  protected void centerY(Widget w) {
    w.moveTo(
        w.getX(),
        (getHeight() - w.getHeight()) / 2f);
  }
}
