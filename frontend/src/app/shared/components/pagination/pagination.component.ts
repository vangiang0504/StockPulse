import { Component, EventEmitter, Input, Output } from '@angular/core';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';

@Component({
  selector: 'app-pagination',
  standalone: true,
  imports: [MatPaginatorModule],
  template: `
    <mat-paginator
      [length]="totalElements"
      [pageSize]="pageSize"
      [pageSizeOptions]="pageSizeOptions"
      (page)="pageChange.emit($event)">
    </mat-paginator>
  `
})
export class PaginationComponent {
  @Input() totalElements = 0;
  @Input() pageSize = 20;
  @Input() pageSizeOptions = [10, 20, 50];
  @Output() pageChange = new EventEmitter<PageEvent>();
}
