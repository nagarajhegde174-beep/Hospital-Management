export interface TimeSlot {
  startTime: string;
  booked: boolean;   // backend serialises boolean isBooked field as "booked" (Jackson strips "is" prefix)
}
