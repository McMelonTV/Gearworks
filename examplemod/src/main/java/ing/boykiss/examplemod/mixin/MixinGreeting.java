package ing.boykiss.examplemod.mixin;

import ing.boykiss.gearworks.common.Greeting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Greeting.class)
public class MixinGreeting {
    @Inject(method = "getGreeting", at = @At("RETURN"), cancellable = true)
    private static void getGreeting(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Hello modded instance");
    }

    @Inject(method = "getGoodbye", at = @At("RETURN"), cancellable = true)
    private static void getGoodbye(CallbackInfoReturnable<String> cir) {
        cir.setReturnValue("Goodbye modded instance");
    }
}
