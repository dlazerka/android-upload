# Android Upload Progress
Example Android application that uploads a file while reporting progress to UI.

Code is pretty clean, feel free to check it out, use, modify, sell and get rich.

Communication between upload progress Activity and uploader IntentService is made using ReactiveX observables. Rx allows us to "debounce" too frequent progress events, move the events from background to main thread, and in general serves as an event bus.

Note that both Activity and Service have intermittent lifecycles. For example if user rotates device, then Activity is re-created, and once a new upload started -- Service is recreated. Thus, we need to have a separate persistent keeper of upload status, and this is where RxJava comes handy.

All upload functionality is isolated in .upload package.

Module `backend` is a trivial GAE web server that accepts any file and stores it in Blobstore.
