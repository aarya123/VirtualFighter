#include "pebble.h"

#define ACCEL_STEP_MS 100
int i=0;
static Window *window;

static AppTimer *timer;

TextLayer *text_layer;
char text_layer_text[100];
static void timer_callback(void *data) {
    AccelData accel = (AccelData) { .x = 0, .y = 0, .z = 0 };
    accel_service_peek(&accel);    
    snprintf(text_layer_text,100, "x=%d y=%d z=%d\n", accel.x,accel.y,accel.z);
    APP_LOG(APP_LOG_LEVEL_INFO, "%s\n", text_layer_text);
    text_layer_set_text(text_layer, text_layer_text);
    timer = app_timer_register(ACCEL_STEP_MS, timer_callback, NULL);
}

static void window_load(Window *window) {
    text_layer = text_layer_create(GRect(0, 0, 144, 168));
    text_layer_set_background_color(text_layer, GColorClear);
    text_layer_set_text_color(text_layer, GColorBlack);
    text_layer_set_text(text_layer,"this is a test");
    layer_add_child(window_get_root_layer(window), (Layer*)text_layer);
}

static void window_unload(Window *window) {
    text_layer_destroy(text_layer);
}

static void init(void) {
    window = window_create();
    window_set_window_handlers(window, (WindowHandlers) {
      .load = window_load,
      .unload = window_unload
    });
    window_stack_push(window, true);

    accel_data_service_subscribe(0, NULL);

    timer = app_timer_register(ACCEL_STEP_MS, timer_callback, NULL);
}

static void deinit(void) {
    accel_data_service_unsubscribe();
    window_destroy(window);
}

int main(void) {
    init();
    app_event_loop();
    deinit();
}