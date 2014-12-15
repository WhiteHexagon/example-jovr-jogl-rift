package com.sunshineapps.riftexample.thirdparty;

import java.util.Stack;
import java.util.function.Consumer;

import org.saintandreas.math.Matrix4f;

public class MatrixStack extends AbstractTransformable<MatrixStack> {
  public static final MatrixStack MODELVIEW = new MatrixStack();
  public static final MatrixStack PROJECTION = new MatrixStack();
  Stack<Matrix4f> stack = new Stack<>();

  public int size() {
    return stack.size() + 1;
  }

  public MatrixStack withPush(Consumer<MatrixStack> closure) {
    int startSize = size();
    push();
    closure.accept(this);
    pop();
    assert(size() == startSize);
    return this;
  }

  public MatrixStack withPush(Runnable closure) {
    int startSize = size();
    push();
    closure.run();
    pop();
    assert(size() == startSize);
    return this;
  }

  public MatrixStack withPush(Matrix4f m, Runnable closure) {
    withPush(()->{
      set(m);
      closure.run();
    });
    return this;
  }

  public MatrixStack withPushIdentity(Runnable closure) {
    withPush(()->{
      identity();
      closure.run();
    });
    return this;
  }

  public MatrixStack pop() {
    set(stack.pop());
    return this;
  }

  public MatrixStack push() {
    stack.push(getTransform());
    return this;
  }

  public Matrix4f top() {
    return getTransform();
  }

}
