#include "pebble.h"

#define HISTORY 50

static Window *window;

TextLayer *accelText;
char accelTextArr[50];

TextLayer *maxText;
char maxTextArr[50];
int maxX=0, maxY=0, maxZ=0;

TextLayer *minText;
char minTextArr[50];
int minX=0, minY=0, minZ=0;

static void checkMinMax(int x, int y, int z){
    if(x>maxX){
            maxX=x;
        }
        if(y>maxY){
            maxY=y;
        }
        if(z>maxZ){
            maxZ=z;
        }
        if(x<minX){
            minX=x;
        }
        if(y<minY){
            minY=y;
        }
        if(z<minZ){
            minZ=z;
        }
}
static void averageAccel(AccelRawData *data, int num_samples, int *avg){
    if(num_samples==0)
        return;
    for(int i=0;i<num_samples;i++){
        int x=data[i].x,y=data[i].y,z=data[i].z;
        checkMinMax(x,y,z);
        avg[0]+=x;
        avg[1]+=y;
        avg[2]+=z;
    }
    avg[0]/=num_samples;
    avg[1]/=num_samples;
    avg[2]/=num_samples;
}

static void accelSubscriber(AccelRawData *data, uint32_t num_samples, uint64_t timestamp){
    int avg[]={0,0,0};
    averageAccel(data,num_samples,avg);
    snprintf(accelTextArr, 50, "x=%d y=%d z=%d", avg[0],avg[1],avg[2]);
    snprintf(maxTextArr,50,"Max: x=%d y=%d z=%d",maxX,maxY,maxZ);
    snprintf(minTextArr,50,"Min: x=%d y=%d z=%d",minX,minY,minZ);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%d",(int)num_samples);
    APP_LOG(APP_LOG_LEVEL_INFO,"%s",accelTextArr);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%s",maxTextArr);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%s",minTextArr);
    text_layer_set_text(accelText, accelTextArr);
    text_layer_set_text(maxText, maxTextArr);
    text_layer_set_text(minText, minTextArr);
}

static void window_load(Window *window) {
    accelText = text_layer_create(GRect(0, 0, 144, 25));
    maxText=text_layer_create(GRect(0,25,144,25));
    minText=text_layer_create(GRect(0,50,144,25));
    text_layer_set_background_color(accelText, GColorClear);
    text_layer_set_background_color(maxText, GColorClear);
    text_layer_set_background_color(minText, GColorClear);
    text_layer_set_text_color(accelText, GColorBlack);
    text_layer_set_text_color(maxText, GColorBlack);
    text_layer_set_text_color(minText, GColorBlack);
    text_layer_set_text(accelText,"Starting up...");
    text_layer_set_text(maxText,"Starting up...");
    text_layer_set_text(minText,"Starting up...");
    layer_add_child(window_get_root_layer(window), (Layer*)accelText);
    layer_add_child(window_get_root_layer(window), (Layer*)maxText);
    layer_add_child(window_get_root_layer(window), (Layer*)minText);
}

static void window_unload(Window *window) {
    text_layer_destroy(accelText);
    text_layer_destroy(maxText);
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