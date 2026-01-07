package game

import fmt "core:fmt"
import rl "vendor:raylib"

main :: proc() {
    window_size :: [2]i32 { 1024, 576 }

    rl.InitWindow(window_size.x, window_size.y, "Gearworks")
    gear := rl.LoadTexture("gear.png")
    gear_pos : [2]f32

    size :: 4

    for !rl.WindowShouldClose() {
        input : [2]f32

        active_directions : [dynamic]Direction

        for direction in Direction {
            active : bool
            for key, index in direction_keys[direction] {
                if rl.IsKeyDown(key) {
                    append(&active_directions, direction)
                    active = true
                }

                if active == true && index == len(direction_keys[direction]) - 1 {
                    input += direction_vectors[direction]
                }
            }
        }

        if rl.IsKeyDown(.SPACE) {
            input *= 2
        }

        gear_pos += input * rl.GetFrameTime() * 256
        fmt.println(rl.GetFrameTime())

        gear_pos.x = clamp(gear_pos.x, 0, f32(window_size.x - gear.width * size))
        gear_pos.y = clamp(gear_pos.y, 0, f32(window_size.y - gear.height * size))

        rl.BeginDrawing()
        rl.ClearBackground({ 160, 200, 255, 255 })
        rl.DrawTextureEx(gear, gear_pos, 0, size, rl.WHITE)
        rl.EndDrawing()
    }

    rl.CloseWindow()
}

Direction :: enum {
    UP,
    DOWN,
    LEFT,
    RIGHT,
}

direction_vectors := [Direction][2]f32 {
    .UP    = { 0, -1 },
    .DOWN  = { 0, 1 },
    .LEFT  = { -1, 0 },
    .RIGHT = { 1, 0 },
}

direction_keys := [Direction][]rl.KeyboardKey {
    .UP    = { .W, .UP },
    .DOWN  = { .S, .DOWN },
    .LEFT  = { .A, .LEFT },
    .RIGHT = { .D, .RIGHT },
}