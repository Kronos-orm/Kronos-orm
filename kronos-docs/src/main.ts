import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { AppComponent } from './app/app.component';
import { documentationEntryRoute } from './app/application-links';

const entry = documentationEntryRoute(location.pathname, location.search, location.hash);
if (entry) history.replaceState(history.state, '', `/#${entry}`);

bootstrapApplication(AppComponent, appConfig)
  .catch((err) => console.error(err));
