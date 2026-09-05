//! JNI boundary for the Fastpotify Android application.

use std::panic::{AssertUnwindSafe, catch_unwind};

use fastpotify_core::{DemoScreen, UiProfile, demo_snapshot_json};
use jni::{
    JNIEnv,
    objects::{JClass, JString},
    sys::{jint, jstring},
};

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_contractVersion(
    _env: JNIEnv<'_>,
    _class: JClass<'_>,
) -> jint {
    fastpotify_core::CONTRACT_VERSION as jint
}

#[unsafe(no_mangle)]
pub extern "system" fn Java_rocks_fastpotify_android_NativeBridge_demoSnapshotJson(
    mut env: JNIEnv<'_>,
    _class: JClass<'_>,
    profile: JString<'_>,
    screen: JString<'_>,
) -> jstring {
    let result = catch_unwind(AssertUnwindSafe(|| {
        let profile = env.get_string(&profile)?.to_string_lossy().into_owned();
        let screen = env.get_string(&screen)?.to_string_lossy().into_owned();
        let json = demo_snapshot_json(parse_profile(&profile), parse_screen(&screen));
        env.new_string(json)
    }));

    match result {
        Ok(Ok(value)) => value.into_raw(),
        Ok(Err(error)) => {
            let _ = env.throw_new("java/lang/IllegalArgumentException", error.to_string());
            std::ptr::null_mut()
        }
        Err(_) => {
            let _ = env.throw_new(
                "java/lang/IllegalStateException",
                "Fastpotify core panicked while building the demo snapshot",
            );
            std::ptr::null_mut()
        }
    }
}

fn parse_profile(value: &str) -> UiProfile {
    match value {
        "voyah_free" => UiProfile::VoyahFree,
        "phone" => UiProfile::Phone,
        _ => UiProfile::Automatic,
    }
}

fn parse_screen(value: &str) -> DemoScreen {
    match value {
        "playlist" => DemoScreen::Playlist,
        "now_playing" => DemoScreen::NowPlaying,
        _ => DemoScreen::Home,
    }
}
