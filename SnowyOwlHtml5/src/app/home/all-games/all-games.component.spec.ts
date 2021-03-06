import {async, ComponentFixture, TestBed} from '@angular/core/testing';

import {AllGamesComponent} from './all-games.component';

describe('AllGamesComponent', () => {
    let component: AllGamesComponent;
    let fixture: ComponentFixture<AllGamesComponent>;

    beforeEach(async(() => {
        TestBed.configureTestingModule({
            declarations: [AllGamesComponent]
        })
            .compileComponents();
    }));

    beforeEach(() => {
        fixture = TestBed.createComponent(AllGamesComponent);
        component = fixture.componentInstance;
        fixture.detectChanges();
    });

    it('should be created', () => {
        expect(component).toBeTruthy();
    });
});
