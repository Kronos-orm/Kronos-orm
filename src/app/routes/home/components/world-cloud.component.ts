import {Component} from '@angular/core';
import {SharedModule} from "../../../shared.module";
import {NgxEchartsDirective, provideEcharts} from "ngx-echarts";

@Component({
  selector: 'world-cloud',
  imports: [
    SharedModule,
    NgxEchartsDirective
  ],
  template: `
    @if(window){
      <div echarts [options]="wordCloudOptions"></div>
    }
  `,
  standalone: true,
  styles: [],
  providers: [
    provideEcharts()
  ]
})
export class WorldCloudComponent {
  wordCloudOptions: any =
    {
      tooltip: {
        show: true
      },
      series: [{
        name: '关键词云',
        type: 'wordCloud',
        size: ['9%', '50%'],
        sizeRange: [10, 30],
        textRotation: [0, 45, 90, -45],
        rotationRange: [-45, 90],
        gridSize: 8,
        shape: 'diamond',
        drawOutOfBound: false,
        autoSize: {
          enable: true,
          minSize: 6
        },
        textStyle: {
          normal: {
            color: () => {
              return 'rgb(' + [
                Math.round(Math.random() * 160),
                Math.round(Math.random() * 160),
                Math.round(Math.random() * 160)
              ].join(',') + ')';
            }
          },
          emphasis: {
            shadowBlur: 10,
            shadowColor: 'rgba(0, 0, 0, 0.15)'
          }
        },
        data: [
          {value: 100, name: '关键词1'},
          {value: 60, name: '关键词2'},
          {value: 30, name: '关键词3'},
          {value: 20, name: '关键词4'},
        ]
      }]
    }

    window = window
}
