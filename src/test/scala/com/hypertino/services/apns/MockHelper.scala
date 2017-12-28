package org.scalamock

import org.scalamock.context.MockContext

object MockHelper {
  def verify(ctx: MockContext) {
    ctx.callLog foreach ctx.expectationContext.verify _
    if (!ctx.expectationContext.isSatisfied)
      ctx.reportUnsatisfiedExpectation(ctx.callLog, ctx.expectationContext)
  }
}