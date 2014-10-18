#include "pebble.h"

#define ACCEL_STEP_MS 50
#define HISTORY 10

static Window *window;

static AppTimer *timer;

TextLayer *text_layer;
char text_layer_text[100];

static void averageAccel(AccelRawData *data, int num_samples, int *avg){
    if(num_samples==0)
        return;
    for(int i=0;i<num_samples;i++){
        avg[0]+=data[i].x;
        avg[1]+=data[i].y;
        avg[2]+=data[i].z;
    }
    avg[0]/=num_samples;
    avg[1]/=num_samples;
    avg[2]/=num_samples;
}

static void accelSubscriber(AccelRawData *data, uint32_t num_samples, uint64_t timestamp){
    int avg[]={0,0,0};
    averageAccel(data,num_samples,avg);
    snprintf(text_layer_text, 100, "x=%d y=%d z=%d\n", avg[0],avg[1],avg[2]);
    text_layer_set_text(text_layer, text_layer_text);
}

static void window_load(Window *window) {
    text_layer = text_layer_create(GRect(0, 0, 144, 168));
    text_layer_set_background_color(text_layer, GColorClear);
    text_layer_set_text_color(text_layer, GColorBlack);
    text_layer_set_text(text_layer,"Starting up...");
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
    accel_raw_data_service_subscribe(HISTORY, accelSubscriber);
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