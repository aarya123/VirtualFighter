//block - everything decreases
//punch - everything decreases


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

TextLayer *healthText;
char healthTextArr[50];

int averageHistory[][3]={{0,0,0},{0,0,0},{0,0,0}};
int counter=0;

/*static int abs(int x){
    if(x<0)
        return -x;
    else
        return x;
}*/

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

static int getPrevCounter(){
    if(counter==0)
        return 2;
    else
        return counter-1;
}

static int getNextCounter(){
    if(counter==2)
        return 0;
    else
        return counter+1;
}

static int isPunch(){
    if(averageHistory[getPrevCounter()][0]-averageHistory[counter][0]>100&&averageHistory[getNextCounter()][0]-averageHistory[counter][0]>100)
        return true;
    else
        return false;
}

static int isBlock(){
    if(averageHistory[getPrevCounter()][2]-averageHistory[counter][2]>100&&averageHistory[getNextCounter()][2]-averageHistory[counter][2]>100)
        return true;
    else
        return false;
}

void send_int(uint8_t key, uint8_t cmd)
{
    DictionaryIterator *iter;
    app_message_outbox_begin(&iter);
    Tuplet value = TupletInteger(key, cmd);
    dict_write_tuplet(iter, &value);
    app_message_outbox_send();
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
    averageHistory[counter][0]=avg[0];
    averageHistory[counter][1]=avg[1];
    averageHistory[counter][2]=avg[2];
    //Because I'm an idiot
    counter--;
    if(isBlock()){
        APP_LOG(APP_LOG_LEVEL_INFO,"block!");
        send_int(0,0);
    }
    else if(isPunch()){
        APP_LOG(APP_LOG_LEVEL_INFO,"punch!");
        send_int(0,1);
    }
    counter+=2;
    if(counter>=3){
        counter=0;
    }
}

static void accelSubscriber(AccelRawData *data, uint32_t num_samples, uint64_t timestamp){
    int avg[]={0,0,0};
    averageAccel(data,num_samples,avg);
    snprintf(accelTextArr, 50, "x=%d y=%d z=%d", avg[0],avg[1],avg[2]);
    snprintf(maxTextArr,50,"Max: x=%d y=%d z=%d",maxX,maxY,maxZ);
    snprintf(minTextArr,50,"Min: x=%d y=%d z=%d",minX,minY,minZ);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%d",(int)num_samples);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%s",accelTextArr);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%s",maxTextArr);
    // APP_LOG(APP_LOG_LEVEL_INFO,"%s",minTextArr);
    text_layer_set_text(accelText, accelTextArr);
    text_layer_set_text(maxText, maxTextArr);
    text_layer_set_text(minText, minTextArr);
}

void process_tuple(Tuple *t)
{
    //int key = t->key;
    APP_LOG(APP_LOG_LEVEL_INFO,"Got something!");
    int value = t->value->int32;
    APP_LOG(APP_LOG_LEVEL_INFO,"health=%d",value);
    snprintf(healthTextArr, 50, "Health=%d", value);
    text_layer_set_text(healthText, healthTextArr);
}

static void in_received_handler(DictionaryIterator *iter, void *context)
{
    Tuple *t = dict_read_first(iter);
    while(t != NULL)
    {
        process_tuple(t);
        t = dict_read_next(iter);
    }
}

static void window_load(Window *window) {
    accelText = text_layer_create(GRect(0, 0, 144, 25));
    maxText=text_layer_create(GRect(0,25,144,25));
    minText=text_layer_create(GRect(0,50,144,25));
    healthText=text_layer_create(GRect(0,75,144,25));
    text_layer_set_background_color(accelText, GColorClear);
    text_layer_set_background_color(maxText, GColorClear);
    text_layer_set_background_color(minText, GColorClear);
    text_layer_set_background_color(healthText, GColorClear);
    text_layer_set_text_color(accelText, GColorBlack);
    text_layer_set_text_color(maxText, GColorBlack);
    text_layer_set_text_color(minText, GColorBlack);
    text_layer_set_text_color(healthText, GColorBlack);
    text_layer_set_text(accelText,"Starting up...");
    text_layer_set_text(maxText,"Starting up...");
    text_layer_set_text(minText,"Starting up...");
    text_layer_set_text(healthText,"Starting up...");
    layer_add_child(window_get_root_layer(window), (Layer*)accelText);
    layer_add_child(window_get_root_layer(window), (Layer*)maxText);
    layer_add_child(window_get_root_layer(window), (Layer*)minText);
    layer_add_child(window_get_root_layer(window), (Layer*)healthText);
}

static void window_unload(Window *window) {
    text_layer_destroy(accelText);
    text_layer_destroy(maxText);
    text_layer_destroy(minText);
    text_layer_destroy(healthText);
}

static void init(void) {
    window = window_create();
    window_set_window_handlers(window, (WindowHandlers) {
      .load = window_load,
      .unload = window_unload
    });
    app_message_register_inbox_received(in_received_handler);
    app_message_open(app_message_inbox_size_maximum(), app_message_outbox_size_maximum());  
    accel_raw_data_service_subscribe(HISTORY, accelSubscriber);
    window_stack_push(window, true);
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