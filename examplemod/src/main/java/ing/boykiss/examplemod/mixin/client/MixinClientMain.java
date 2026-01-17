package ing.boykiss.examplemod.mixin.client;

import ing.boykiss.gearworks.client.ClientMain;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(ClientMain.class)
public class MixinClientMain {
    @ModifyArgs(method = "loop", at = @At(value = "INVOKE", target = "Lorg/lwjgl/opengl/GL46;glClearColor(FFFF)V"))
    private static void loop(Args args) {
        args.set(0, 0f);
        args.set(1, 1f);
    }
}
